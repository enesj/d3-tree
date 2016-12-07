(ns foo.example.core
  (:require-macros
    [reagent.ratom :refer [reaction]]
    [cljs.core.async.macros :as m :refer [go]])
  (:require
    [reagent.core :as r :refer [atom]]
    [cljsjs.material-ui]
    [cljs-react-material-ui.core :as ui]
    [cljs-react-material-ui.reagent :as rui]
    [cljs-react-material-ui.icons :as ic]
    [ajax.core :refer [GET POST json-response-format json-request-format url-request-format ajax-request]]
    [cljs.core.async :refer [chan close! timeout]]
    [cljsjs.d3]
    [devtools.core :as devtools]
    [devtools.toolbox :as toolbox]))

(enable-console-print!)

(devtools/install!)

(defonce db-tree (atom nil))
(def data-flare (clj->js []))
(def scroll-data (atom nil))
(def search-data (atom {:selection nil :childs nil :parents nil :refresh true :search-type 0 :graphics false :history []}))
(def show-ac (atom {:ac "text" :change true}))


(def margin {:top 24, :right 20, :bottom 30, :left 30})
(def width (- 800 (:left margin) (:right margin)))
(def barHeight 17)
(def barWidth (* width .7))
(def duration 400)
(def title-lenght 140)
(def y-chars-ratio 5)

(def colors
  {:blue900   (ui/color :blue900)
   :blue500   (ui/color :blue500)
   :red500    (ui/color :red500)
   :lightgrey (ui/color :grey400)
   :grey      (ui/color :grey600)
   :darkgrey  (ui/color :grey800)
   :cyan500   (ui/color :cyan500)})

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


  (d3-tree d
           ;(if ctrl (do (sel-data (.-name d)) (js/console.log "ctrl"))
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
        (on "mouseenter" (fn [d] (reset! scroll-data d) (scroll-title)))
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
    (.. node-group
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

(defn select-type-change [chosen]
  (swap! search-data assoc-in [:search-type] chosen))

(defn clear-criteria [refresh] (reset! search-data {:selection   nil :childs nil :parents nil :refresh false
                                                    :search-type (:search-type @search-data) :graphics (:graphics @search-data) :history (:history @search-data)})
  (if refresh (swap! show-ac update-in [:change] not)))

(defn update-history [id]
  (conj (remove #{id} (:history @search-data)) id))

(defn remove-from-history [id]
  (swap! search-data assoc-in [:history] (remove #{id} (:history @search-data))))

(defn sel-data [doc]
  (GET "/jus/search-data" {:params        {:doc doc}
                           :handler       (fn [x]
                                            (reset! search-data {:refresh     false :parents (first x) :childs (second x) :selection doc
                                                                 :search-type (:search-type @search-data) :graphics (:graphics @search-data) :history (update-history doc)}))
                           :error-handler #(js/alert (str "error: " %))}))

(defn select-doc-type [hide]
  [rui/select-field {:id        "type"
                     :autoWidth true
                     :disabled  hide
                     :style     {
                                 ;:width "20%"  :min-width "230px" :vertical-align "middle"
                                 :font-size   "12px" :display "inline-block" :margin-right "30px"
                                 :font-family "Roboto, sans-serif" :font-weight "Bold"}
                     ;:floatingLabelStyle {:font-size "16px"}
                     :value     (:search-type @search-data)
                     ;:hint-text           "Vrsta pretrage"
                     :on-change (fn [event index value]
                                  (clear-criteria true)
                                  (select-type-change value))}
   [rui/menu-item {:value 0 :primary-text "Svi dokumenti"}]
   [rui/menu-item {:value 1 :primary-text "BiH naredbe"}]
   [rui/menu-item {:value 2 :primary-text "YU naredbe/pravilnici"}]
   [rui/menu-item {:value 3 :primary-text "JUS standardi"}]])

(defn ac1 [source]
  [rui/auto-complete {:id                  "oznaka"
                      :floating-label-text "Oznaka JUS standarda"
                      :openOnFocus         true
                      ;:floating-label-style {:text-align "center"}
                      :input-style         {:width "100%" :text-align "center" :font-weight "bold"}
                      :dataSource          source
                      :maxSearchResults    20
                      :filter              (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
                      :on-new-request      (fn [chosen] (sel-data chosen))
                      :hint-text           "Unesi oznaku JUS-a"
                      :full-width          true
                      :list-style          {:height "250px"}}])

(defn ac2 [source]
  (let [source-filter (case (:search-type @search-data) 0 nil 1 #(= 1 (:type %)) 2 #(> (:type %) 1) 3 #(= 0 (:type %)))
        source-new (if source-filter (mapv #(dissoc % :type) (filterv source-filter source)) source)]

    [:div
     [rui/auto-complete {:id                  "text"
                         :floating-label-text "Naziv"
                         :openOnFocus         true
                         :input-style         {:width "100%" :text-align "center" :font-weight "bold"}
                         ;:style               {:display "inline-block" :width "75%" :padding-right "30px"}
                         :dataSource          source-new
                         :maxSearchResults    50
                         :filter              (aget js/MaterialUI "AutoComplete" "fuzzyFilter")
                         :full-width          true
                         :on-new-request      (fn [chosen index] (sel-data (:value (source-new index))))
                         :hint-text           "Unesi dio teksta iz naslova"
                         :list-style          {:height "250px" :width "300%"}}]]))
;(select-doc-type)]))

(def legend-data
  {:default {:width  10
             :height 10
             :rx     2
             :ry     2}
   :items   [{:id "bih" :type :rect :x 0 :y 50 :fill (:blue500 colors)}
             {:id "bih-t" :type :text :x 20 :y 60 :text "BiH naredbe"}
             {:id "yu" :type :rect :x 120 :y 50 :fill (:red500 colors)}
             {:id "yu-t" :type :text :x 140 :y 60 :text "YU naredbe"}
             {:id "jus1" :type :rect :x 0 :y 80 :fill (:darkgrey colors)}
             {:id "jus1-t" :type :text :x 20 :y 90 :text "JUS sa obaveznom primjenom"}
             {:id "jus2" :type :rect :x 0 :y 100 :fill (:grey colors)}
             {:id "jus2-t" :type :text :x 20 :y 110 :text "JUS sa djelimično obaveznom primjenom"}
             {:id "jus3" :type :rect :x 0 :y 120 :fill (:lightgrey colors)}
             {:id "jus3-t" :type :text :x 20 :y 130 :text "JUS za upotrebu"}
             ;{:id "sel" :type :rect :x 0  :y 120 :fill "white" :stroke "black"}
             {:id "sel-t" :type :text :x 0 :y 160 :font-weight "bold" :text "Naredbe\\standardi koji sadrže rezultat pretrage" :fill (:cyan500 colors)}]})

(defn legend []
  [:svg {:style    {:width "100%" :max-height "200px" :font-size "14px" :padding-left "15px" :padding-top "5px" :margin-top "12px" :border-style "ridge" :border-radius "10px" :border-color (:cyan500 colors)}
         :view-box [0 0 330 180]}
   [:g
    [:text {:x 0 :y 20 :font-size "20px" :fill (:blue500 colors)} "Legenda"]
    (for [item (:items legend-data)]
      ^{:key (:id item)} [(:type item) (merge (:default legend-data) (dissoc item :text :type))
                          (if (= (:type item) :text) (:text item))])]])

(defn label-text [label text width]
  [:div {:style (merge {:width width :display "inline-block"}
                       (if (= "" label) {:font-weight "bold" :text-align "center"}))}
   [:span {:style {:font-weight "bold" :color (:cyan500 colors)}} label]
   text])

(defn label-text-wide [label data]
  [rui/css-transition-group {:transition-name          "example1"
                             :transition-enter-timeout 600
                             :transition-leave-timeout 500}
   [:div {:class "foo1"
          :style {:text-align   "center" :font-weight "bold" :margin-bottom "15px" :border-bottom-style "ridge"
                  :border-color (:cyan500 colors)
                  :color        (case (:Naredba data) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                                                      (case (:Mandatory data) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow")}} (:title data)]])

(defn yu-naredba-view [data]
  (let [width "50%"]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "YU naredba/pravilnik" width)
     (label-text "Službeni glasnik: " (:Glasnik data) width)]))

(defn bh-naredba-view [data]
  (let [width "25%"]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     ;[:div {:style {:font-weight "bold" :margin-bottom "8px" :margin-top "8px" }} (:title data)]
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "BiH naredba" width)
     (label-text "Službeni glasnik: " (:Glasnik data) width)
     (label-text "Evropska direktiva: " [:a {:href (:Link-d data) :target "_blank"} (:Direktiva data)] width)
     [:a {:href (str "pdf/" (:Link-n data)) :target "_blank" :style {:font-weight "bold" :color (:cyan500 colors)}} "Prikaži dokument"]]))

(defn jus-view [data]
  (let [width "15%"]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     (label-text-wide "Naziv: " data)
     (label-text "" (str (:name data) ":" (:JUSgodina data)) width)
     (label-text "Vrsta: " "JUS standard" width)
     (label-text "Godina: " (:JUSgodina data) width)
     (label-text "Primjena: " (case (:Mandatory data) 0 "Obavezna" 1 "Djelimično obavezna" 2 "Za upotrebu") width)
     (label-text "Broj strana: " (:Strana data) width)
     (label-text "ICS: " (:ICS data) width)]))

(defn connected-docs [docs header]
  (let [docs (sort-by #(case (:Naredba %) 1 1 2 2 3 3 0 4) < docs)
        color (fn [doc] (case (:Naredba doc) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                                             (case (:Mandatory doc) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow"))]
   [rui/table {:selectable false :height (str (- (if (< (count docs) 5) (* (count docs) 50) 250) 10) "px") :on-cell-click (fn [row coll] (if (= coll 1)(sel-data (or (:JUSId (nth docs row)) (:name (nth docs row))))))}
    [rui/table-header {:display-select-all false}
     [rui/table-row
      [rui/table-header-column {:col-span 2 :style {:font-weight "bold" :font-size "16px" :text-align "center" :margin-bottom "5px" :color (:cyan500 colors)}}  header]]]
    [rui/table-body {:display-row-checkbox false :pre-scan-rows false}
        (for [doc docs]
          [rui/table-row {:key (or (:JUSId doc) (:name doc))  :style {:font-weight "normal" :cursor "link" :color (color doc)}}
           [rui/table-row-column {:style {:width "90%"}}
            [:span {:style {:font-weight "bold"}}
             (if (= 0 (:Naredba doc)) (str (or (:JUSId doc) (:name doc)) ":" (:JUSgodina doc) "  "))]
            (or (:JUSopis doc) (:title doc))]
           [rui/table-row-column {:style {:width "10%"}}
            (if (:name doc) [rui/icon-button {:tooltip    "Brisi iz istorije"
                                              :tooltip-position "top-left"
                                              :tooltip-styles {:margin-top "30px" :width "80px" :right "20px"}
                                              :on-click   #(remove-from-history (:name doc))
                                              :style      {:width "24px" :height "24px" :float "right"}
                                              :icon-style {:width "20px" :height "20px" :color (:cyan500 colors)}} (ic/content-clear)])]])]]))

(defn search-result []
  (let [search @search-data
        result (first (filter #(= (:selection search) (:name %)) @db-tree))
        childs (second (:childs search))
        parents (second (:parents search))
        type (:Naredba result)]
    [:div {:style {:font-size "16px" :margin-bottom "20px"} :key "sr"}
     [rui/icon-button {:tooltip    "Brisi pretragu" :on-click #(clear-criteria true)
                       :style      {:vertical-align "top" :float "right" :margin-top "-40px"}
                       :icon-style {:width "24px" :height "24px" :color (:cyan500 colors)} :tooltip-position "bottom-left"} (ic/content-clear)]
     (case type 0 (jus-view result) 1 (bh-naredba-view result) (yu-naredba-view result))
     [:hr]
     ;[:div {:style {:font-weight "bold" :margin-bottom "5px" :color (:cyan500 colors)}} "Vezani dokumenti:"]
     ;[:div {:style {:margin-top "10px" :max-height "250px" :overflow-y "scroll"}}
     (connected-docs childs "Vezani dokumenti:")
     [:hr]
     ;[:div {:style {:font-weight "bold" :margin-bottom "5px" :color (:cyan500 colors)}} "Vezan za dokumente:"]
     ;[:div {:style {:margin-top "10px" :max-height "250px" :overflow-y "scroll" :class "foo"}}
     (connected-docs parents "Vezan za dokumente:")]))

(defn history []
  (let [list (take 10 (:history @search-data))
        history-data (fn [x] (first (filter #(= x (:name %)) @db-tree)))]
    (connected-docs (map #(history-data %) list) "Istorija pretraživanja")))

(defn home-page [source source-text]
  (let [search-d @search-data
        show @show-ac]
    [rui/mui-theme-provider {:mui-theme (ui/get-mui-theme {:palette {:text-color (:blue500 colors)}})}
     [:div
      [rui/app-bar {:title              "eJUS"
                    ;:icon-element-left (ui/icon-button (ic/action-account-balance-wallet))
                    :showMenuIconButton false
                    :zDepth             3
                    :icon-element-right (ui/icon-button
                                          {:on-click #(swap! search-data assoc-in [:graphics] true)
                                           :tooltip  "Grafički prikaz"
                                           :children (ic/editor-insert-chart)
                                           :disabled (:graphics search-d)})
                    :style              {:background-color (:blue900 colors)}}]
      [rui/paper {:z-depth 2 :class-name "col-md-12" :style {:margin-top "10px"}}
       [rui/css-transition-group {:transition-name          "example"
                                  :transition-enter-timeout 600
                                  :transition-leave-timeout 500}
        (if-not (:selection search-d)
          [:div {:class "foo" :key "rbg"}
           [:div {:style {:padding "3px 0px 0px 0px" :margin-top "10px" :border-style "ridge" :border-radius "10px" :border-color (:cyan500 colors)}}
            [rui/radio-button-group {:name  "pretraga" :default-selected (:ac show) :on-change (fn [_ value]
                                                                                                 (swap! show-ac assoc-in [:ac] value)
                                                                                                 (clear-criteria false))
                                     :style {:font-size "12px" :display "inline-block" :width "40%" :margin-right "30px" :padding "0px 0px 0px 5px"}}
             ; :border-style "ridge" :border-radius "10px" :border-color (:cyan500 colors)}}
             [rui/radio-button {:value "oznaka" :label "Traži po oznaci JUS-a" :style {:display "inline-block" :width "50%" :min-width "190px" :vertical-align "top"}}]
             [rui/radio-button {:value "text" :label "Traži po tekstu naziva" :style {:display "inline-block" :width "50%" :min-width "190px" :vertical-align "top"}}]]
            (if-not (= (:ac show) "oznaka") (select-doc-type false) (select-doc-type true))]
           (if (= (:ac show) "oznaka")
             [:div (if (:change show) [:div {} (ac1 source)] (ac1 source))]
             [:div (if (:change show) [:div {} (ac2 source-text)] (ac2 source-text))])
           [rui/paper {:z-depth 2 :class-name "col-md-12" :style {:margin-top "20px" :margin-bottom "20px"}}
            ;[:div {:style {:font-weight "bold" :font-size "16px" :margin-bottom "20px" :color (:cyan500 colors)}} "Istorija pretraživanja:"]
            ;[:hr {:style {:margin-top "0px" :margin-bottom "20px"}}]
            [:div (history)]]]
          (search-result))]]
      [rui/paper {:class-name "col-md-12" :z-depth 4 :style {:margin-top "20px" :position "absolute" :top "580px" :max-width "98%" :display (if-not (:graphics search-d) "none")}} ;}}
       [rui/icon-button {:tooltip          "Zatvori"
                         :tooltip-position "bottom-left"
                         :on-click         #(swap! search-data assoc-in [:graphics] false)
                         :style            {:vertical-align "top" :float "right"}
                         :icon-style       {:color (:cyan500 colors)}} (ic/content-clear)]
       [:div {:class-name "col-md-8" :style {:font-size "20px" :margin-top "12px" :display "inline-block"}} "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
        ;[:div (legend)]
        [:div {:id "app" :style {:max-height "500px" :overflow "auto"}}]]
       [:div {:class-name "col-md-3" :style {:font-size "20px" :display "inline-block"}} (legend)]]]]))



(defn mount-root []
  (r/render
    [home-page (mapv :name @db-tree) (mapv #(hash-map :text (:title %) :value (:name %) :type (:Naredba %)) @db-tree)]
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

;(init-veza)

(defn main []
  (init-veza))

