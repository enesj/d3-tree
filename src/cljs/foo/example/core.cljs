(ns foo.example.core
  (:require-macros
    [reagent.ratom :refer [reaction]]
    [cljs.core.async.macros :as m :refer [go]])

  (:require
    [reagent.core :as r :refer [atom]]
    ;[re-com.core :as re-com :refer [h-box v-box box gap line row-button md-circle-icon-button label checkbox horizontal-bar-tabs vertical-bar-tabs title p
    ;                                scroller single-dropdown button alert-box v-split h-split modal-panel]
    ; :refer-macros [handler-fn]]
    ;[re-com.popover :refer [popover-tooltip]]
    [cljsjs.material-ui]
    [cljs-react-material-ui.core :as ui]
    [cljs-react-material-ui.reagent :as rui]
    [cljs-react-material-ui.icons :as ic]
    ;[rum.core :as rum]
    [ajax.core :refer [GET POST json-response-format json-request-format url-request-format ajax-request]]
    [cljs.core.async :refer [chan close! timeout]]
    [cljsjs.d3]
    cljsjs.react-autosuggest
    [devtools.core :as devtools]
    [devtools.toolbox :as toolbox]))
    ;[foo.example.autosuggest :as auto]
    ;[foo.example.select :as select]))


;[devtools.formatters.core :as format]
;[devtools.formatters.templating :refer [make-template]]
;[devtools.protocols :refer [IFormat]]
;[dirac.runtime :as dirac]


(enable-console-print!)

(devtools/install!)

(def db-tree (atom nil))
(def scroll-data (atom nil))
(def search-data (atom {:selection nil :childs nil :parents nil :refresh true}))
(def click-delay (atom 0))
(def data-flare (clj->js []))

(def margin {:top 24, :right 20, :bottom 30, :left 30})
(def width (- 800 (:left margin) (:right margin)))
(def barHeight 17)
(def barWidth (* width .7))
(def duration 400)
(def title-lenght 140)
(def y-chars-ratio 5)

(def colors
     {:blue900 (ui/color :blue900)
      :blue500 (ui/color :blue500)
      :red500 (ui/color :red500)
      :lightgrey (ui/color :grey400)
      :grey (ui/color :grey600)
      :darkgrey (ui/color :grey800)
      :cyan500 (ui/color :cyan500)})



(defonce svg nil)

(defn mount-svg []
  (set! svg
        (.. js/d3
            (select "#app")
            (append "svg:svg")
            (attr "width" (+ width (:left margin) (:right margin)))
            (attr "id" "svg-tree")
            (append "svg:g")
            (append "svg:g")
            (attr "transform" (str "translate(" (:left margin) "," (:top margin) ")")))))

(defn sel-data [doc]
  (GET "/jus/search-data" {:params        {:doc doc}
                           :handler       (fn [x]
                                            (swap! search-data assoc-in [:refresh] false)
                                            (swap! search-data assoc-in [:parents] (first x))
                                            (swap! search-data assoc-in [:childs] (second x))
                                            (swap! search-data assoc-in [:selection] doc))
                           :error-handler #(js/alert (str "error: " %))}))

(add-watch search-data :update-tree
           (fn [key atom old-state new-state]

             (if (= false (:refresh @search-data))
               (doseq [doc (merge (first (:parents old-state)) (:selection old-state))]
                 (.. js/d3
                     (selectAll "textPath")
                     (filter (fn [d i] (if (= (.-name d) doc) (js* "this") nil)))
                     (style "fill" (:darkgray colors)))))
             (if (not-empty (first (:parents new-state)))
               (doseq [doc (merge (first (:parents new-state)) (:selection new-state))]
                 (.. js/d3
                     (selectAll "textPath")
                     (filter (fn [d i] (if (= (.-name d) doc) (js* "this") nil)))
                     (style "fill" (:cyan500 colors)))))))


(defn all-childs [parent]
  (GET "/jus/childs" {:params        {:parent parent}
                      :handler       (fn [x] (swap! search-data assoc-in [:childs] x))
                      :error-handler #(js/alert (str "error: " %))}))

(defn all-parents [child]
  (GET "/jus/parents" {:params        {:child child}
                       :handler       (fn [x] (swap! search-data assoc-in [:parents] x))
                       :error-handler #(js/alert (str "error: " %))}))

(defn expand-first-level [d]
  (set! (.-children d) (.-_children d)) (set! (.-_children d) nil))



(defn scroll-title []
  (let [s-data @scroll-data
        id (.-id s-data)
        y (.-y s-data)
        text-lenght (.. js/d3
                        (select (str "#" (str "text" id)))
                        (select "textPath")
                        (text (.-title s-data))
                        (node)
                        (getComputedTextLength))
        correction (if (= (.-shorttitle s-data) "") 480 400)]
    (if (< (- correction text-lenght y) 0)
      (.. js/d3
          (select (str "#" (str "text" id)))
          (select "textPath")
          transition
          (delay 600)
          (duration (* text-lenght 3))
          (ease "linear")
          (attr "startOffset" (fn [d] (if (= id (.-id @scroll-data)) (- correction text-lenght y) 0)))))))


(def diagonal
  (.. js/d3
      -svg
      diagonal
      (projection (fn [d] (clj->js [(.-y d) (.-x d)])))))

(def tree
  (.. js/d3 -layout tree
      (nodeSize (clj->js [0, 20]))))


(defn collapse [d]
  (let [children (.-children d)]
    (if children (do (set! (.-_children d) (.-children d))
                     (set! (.-children d) nil)
                     (mapv #(collapse %) (.-_children d))))))






(defn click-fn [d d3-tree ctrl]
  (if (.-children d) (do (set! (.-_children d) (.-children d)) (set! (.-children d) nil))
                     (do (if (.-_children d)
                           (do (set! (.-children d) (.-_children d)) (set! (.-_children d) nil))
                           (set! (.-children d) (clj->js (:children (first (filter #(= (:name %) (.-name d)) @db-tree))))))))


  (d3-tree d)
  (if ctrl (do (sel-data (.-name d)) (js/console.log "ctrl"))
           (swap! search-data assoc-in [:refresh] true)))


(defn d3-tree [data-new]
  (let [tree-nodes (.. tree
                       (nodes data-flare))
        parents (first (:parents @search-data))
        height (+ (* (.-length tree-nodes) barHeight) (:top margin) (:bottom margin))
        node (.. svg
                 (selectAll "g.node")
                 (data tree-nodes (fn [d i]
                                    (or (.-id d) (set! (.-id d)
                                                       (str (clojure.string/replace (.-name d) "." "") (rand-int 1000)))))))
        link (.. svg
                 (selectAll "path.link")
                 (data (.. tree (links tree-nodes)) (fn [d] (.-id (.-target d)))))
        node-group (.. node
                       enter
                       (append "svg:g")
                       (attr "class" "node")
                       (attr "transform" (fn [d i] (str "translate(" (.-y0 data-new) "," (.-x0 data-new) ")")))
                       (style "opacity" 1))]

    (.. js/d3 (select "#svg-tree")
        transition
        (duration duration)
        (attr "height" height))

    (.. tree-nodes (forEach (fn [d i] (set! (.-x d) (* i 17)))))
    (.. node-group
        (append "svg:rect")
        (attr "x" "-5")
        (attr "y" "-5")
        (attr "height" 10)
        (attr "width" 10)
        (style "stroke" "none")
        (attr "class" "node-dot")
        (style "fill-opacity" 1)
        (style "fill" (fn [d i] (case (.-type d) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                                                 (case (.-mandatory d) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow"))))
    (.. node-group
        (append "rect")
        (attr "y" (- 5))
        (attr "height" 10)
        (attr "width" width)
        (attr "opacity" 1e-6)
        (on "click" (fn [d] (click-fn d d3-tree (.. js/d3 -event -ctrlKey))))
        (on "mouseenter" (fn [d] (when (> (- (.getTime (js/Date.)) (or @click-delay 0)) 300) (reset! scroll-data d) (scroll-title))))
        (on "mouseout" (fn [d] (reset! scroll-data false) (.. js/d3 (select (str "#" "text" (.-id d)))
                                                              (select "textPath")
                                                              (text (if (> (count (.-title d)) (- title-lenght (int (/ (.-y d) y-chars-ratio)) (if (= (.-shorttitle d) "") 0 (/ 100 y-chars-ratio))))
                                                                      (apply str (concat (take (- title-lenght (int (/ (.-y d) y-chars-ratio)) (if (= (.-shorttitle d) "") 0 (/ 100 y-chars-ratio))) (.-title d)) "..."))
                                                                      (.-title d)))
                                                              transition
                                                              (duration 1000)
                                                              (attr "startOffset" 0)))))
    (.. node-group
        (append "svg:path")
        (attr "id" (fn [d i] (str "path" (.-name d))))
        (attr "stroke-width" 2)
        (attr "stroke" (fn [d i]
                         (case (.-type d) 1 "yellow" 2 "white" 3 "white" 0 (case (.-mandatory d) 2 "gray" 1 "white" 0 "white") "blue")))
        (attr "d" (fn [d i]
                    (if (and (:children (first (filter #(= (:name %) (.-name d)) @db-tree))) (not (.-children d)))
                      "m-3 0 l6 0 m-3 -3 l0 6"
                      (if (.-children d) "m-3 0 l6 0")))))
    (.. node-group
        (append "svg:path")
        (attr "id" (fn [d i] (str "path" (.-id d))))
        (attr "d" (fn [d] (if (= (.-shorttitle d) "") "m7 2 1000 0" "m100 5 1000 0"))))
    (.. node-group
        (append "svg:text")
        (attr "id" (fn [d i] (str "text" (.-id d))))
        ;(attr "text-anchor" "start")
        (attr "dx" 7)
        (attr "dy" (fn [d] (if (= (.-shorttitle d) "") 2 5)))
        (style "font-weight" "bold")
        (text (fn [d] (.-shorttitle d)))
        (append "textPath")
        (attr "xlink:href" (fn [d] (str "#path" (.-id d))))
        (attr "startOffset" 0)
        (style "font-weight" (fn [d] (if (= (.-shorttitle d) "") "bold" "normal")))
        (text (fn [d i]
                (if (> (count (.-title d)) (- title-lenght (int (/ (.-y d) y-chars-ratio)) (if (= (.-shorttitle d) "") 0 (/ 100 y-chars-ratio))))
                  (apply str (concat (take (- title-lenght (int (/ (.-y d) y-chars-ratio)) (if (= (.-shorttitle d) "") 0 (/ 100 y-chars-ratio))) (.-title d)) "..."))
                  (.-title d)))))
    ;(each (fn [d i] (doseq [title (for [title-part (partition 2 1 (range 0 500 50) )]
    ;                       (.substring (.-title d) (first title-part) (second title-part)) )]
    ;          (.. js/d3
    ;              (select (js* "this"))
    ;              (append "tspan")
    ;              (text title)
    ;              (attr "x" 0)
    ;              (attr "dy" 15)))))

    (.. node-group
        ;(style "font-weight" (fn [d] (js/console.log (str (.-name d) parents))  (if (or parents #{}) (.-name d) ) "bold" "normal" ))
        transition
        (duration duration)
        (attr "transform" (fn [d i] (str "translate(" (.-y d) "," (.-x d) ")")))
        (style "opacity" 1))
    (.. node
        transition
        (duration duration)
        (attr "transform" (fn [d i]
                            (str "translate(" (.-y d) "," (.-x d) ")")))
        (style "opacity" 1))
    (.. node
        (select "path")
        (attr "d" (fn [d i]
                    (if (and (:children (first (filter #(= (:name %) (.-name d)) @db-tree))) (not (.-children d)))
                      "m-3 0 l6 0 m-3 -3 l0 6"
                      (if (.-children d) "m-3 0 l6 0")))))
    (.. node
        exit
        transition
        (duration duration)
        (attr "transform" (fn [d i] (str "translate(" (.-y data-new) "," (.-x data-new) ")")))
        (style "opacity" 1e-6)
        remove)
    (.. link
        enter
        (append "svg:path" "g")
        (attr "class" "link")
        (style "opacity" 0.5)
        (attr "d" (fn [d] (if (.-x0 data-new) (diagonal (clj->js {:source {:x (.-x0 data-new) :y (.-y0 data-new)} :target {:x (.-x0 data-new) :y (.-y0 data-new)}})))))
        transition
        (duration duration)
        (attr "d" diagonal))
    (.. link
        transition
        (duration duration)
        (attr "d" diagonal))
    (.. tree-nodes (forEach (fn [d] (set! (.-x0 d) (.-x d)) (set! (.-y0 d) (.-y d)))))
    (.. link
        exit
        transition
        (duration duration)
        (attr "d" (fn [d] (diagonal (clj->js {:source {:x (.-x0 data-new) :y (.-y0 data-new)} :target {:x (.-x0 data-new) :y (.-y0 data-new)}}))))
        remove)))

(def show-ac (atom {:ac "oznaka" :refresh true}))

(defn ac1 [source]
  [rui/auto-complete {:id "oznaka"
                      :floating-label-text  "Oznaka JUS standarda"
                      ;:floating-label-fixed true
                      :dataSource           source
                      :maxSearchResults 20
                      :filter               (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
                      :on-new-request       (fn [chosen] (sel-data chosen))
                      :hint-text            "Unesi oznaku JUS-a"
                      :full-width           true
                      :list-style           {:height "250px"}}])

(defn ac2 [source]
  [rui/auto-complete {:id "text"
                      :floating-label-text  "Tekst iz naziva standarda/naredbe"
                      ;:floating-label-fixed true
                      :dataSource           source
                      :maxSearchResults 50
                      :filter               (aget js/MaterialUI "AutoComplete" "fuzzyFilter")
                      :full-width           true
                      :on-new-request       (fn [chosen index] (sel-data (:value (source index))))
                      :hint-text            "Unesi dio teksta iz naslova"
                      :list-style           {:height "250px" :width "300%"}}])

(def legend-data
  {:default {:width            10
             :height           10
             :rx               2
             :ry               2}
   :items [{:id "bih" :type :rect :x 0  :y 20 :fill (:blue500 colors)}
           {:id "bih-t" :type :text :x 20 :y 30 :text "BiH naredbe"}
           {:id "yu" :type :rect :x 120  :y 20 :fill (:red500 colors)}
           {:id "yu-t" :type :text :x 140 :y 30 :text "YU naredbe"}
           {:id "jus1" :type :rect :x 0  :y 40 :fill (:darkgrey colors)}
           {:id "jus1-t" :type :text :x 20 :y 50 :text "JUS sa obaveznom primjenom"}
           {:id "jus2" :type :rect :x 0  :y 60 :fill (:grey colors)}
           {:id "jus2-t" :type :text :x 20 :y 70 :text "JUS sa djelimično obaveznom primjenom"}
           {:id "jus3" :type :rect :x 0  :y 80 :fill (:lightgrey colors)}
           {:id "jus3-t" :type :text :x 20 :y 90 :text "JUS za upotrebu"}]})


(defn legend []
  [:svg {:style {:width "100%" :height "150px" :font-size "14px"}}
      [:g (for [item (:items legend-data)]
           ^{:key (:id item)} [(:type item) (merge (:default legend-data) (dissoc item :text :type))
                               (if (= (:type item) :text) (:text item))])]])



(defn home-page [source source-text]
   [rui/mui-theme-provider {:mui-theme (ui/get-mui-theme {:palette {:text-color (:blue500 colors)}})}
     [:div
      [rui/app-bar {:title              "JUS standardi vezani sa EU direktivama usvojenim u BiH "
                    :icon-element-right (ui/icon-button (ic/action-account-balance-wallet))
                    :style              {:background-color (:blue900 colors)}}]

      [:div {:class-name "col-md-5" :style {:margin-top "20px"}}
       [rui/paper {:z-depth 2 :class-name "col-md-12"}
         [:div {:style {:font-size "20px" :display "inline-block" :margin-top "8px" :margin-bottom "6px"}} "Pretraga JUS standarda i harmoniziranih naredbi"]
         [rui/icon-button {:tooltip    "Brisi pretragu" :on-click #((reset! search-data {:selection nil :childs nil :parents nil :refresh false}) (swap! show-ac update-in [:refresh] not))
                           :style      {:vertical-align "top" :float "right"}
                           :icon-style {:width "24px" :height "24px"} :tooltip-position "bottom-left"}  (ic/content-clear)]
         [:div [rui/radio-button-group  {:name "pretraga" :default-selected (:ac @show-ac) :on-change (fn [_ value]  (swap! show-ac assoc-in [:ac] value) (swap! show-ac update-in [:refresh] not))
                                         :style {:font-size "12px"  :display "inline-block" :width "80%"}}
                [rui/radio-button {:value "oznaka" :label "Traži po oznaci JUS-a" :style {:display "inline-block" :width "50%" :vertical-align "top"}}]
                [rui/radio-button {:value "text" :label "Traži po tekstu naziva" :style {:display "inline-block" :width "50%" :vertical-align "top"}}]]]
         (if (= (:ac @show-ac) "oznaka") [:div {} (if (:refresh @show-ac)  [:div {}(ac1 source)] (ac1 source))]
                                         [:div {} (if (:refresh @show-ac)  [:div {}(ac2 source-text)] (ac2 source-text))])]]
      [:div {:class-name "col-md-7" :style {:margin-top "20px"}}]
      [rui/paper {:class-name "col-md-12" :z-depth 4 :style {:margin-top "20px" :position "absolute" :top "600"}}

        [:div {:class-name "col-md-9" :style { :font-size "20px" :margin-top "4px" :display "inline-block"}} "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
         ;[:div (legend)]
         [:div {:id "app" :style {:max-height "500px" :overflow "auto"}}]]
        [:div {:class-name "col-md-3" :style {:font-size "20px" :margin-top "4px" :display "inline-block"}} "Legenda " (legend)]]]])

(def home-page-t
  (with-meta home-page
             {:component-did-update (fn [] (if-not @show-ac (reset! show-ac true)))}))

(defn mount-root []
  (r/render
    ;[home-page (map #(select-keys % [:name]) @db-tree) sel-data]
    [home-page (mapv :name @db-tree)  (mapv #(hash-map :text (:title %) :value (:name %))   @db-tree)]
    (.getElementById js/document "search-app")))


(defn init-veza []
  (GET "/jus/tree" {:handler       (fn [x]
                                     (reset! db-tree x)
                                     (mount-root)
                                     (set! data-flare (clj->js (first (filter #(= (:name %) "1000") @db-tree))))
                                     (mount-svg)
                                     (d3-tree data-flare))
                    ;(collapse data-flare)
                    ;(expand-first-level data-flare)
                    ;(d3-tree data-flare)
                    :error-handler #(js/alert (str "error: " %))}))

(defn main [])
;(mount-root)
(init-veza)

