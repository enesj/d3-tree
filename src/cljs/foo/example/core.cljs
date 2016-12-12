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
(def search-data (atom {:selection nil :veza {:sel nil :childs nil :parents nil } :childs nil :parents nil :refresh true :search-type 0 :graphics false :history []}))
(def show-ac (atom false))


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

(defn clear-criteria [refresh] (reset! search-data {:selection   nil :veza nil :childs nil :parents nil :refresh false
                                                    :search-type (:search-type @search-data) :graphics (:graphics @search-data) :history (:history @search-data)})
  (if refresh (reset! show-ac (not @show-ac))))

(defn update-history [id]
  (conj (remove #{id} (:history @search-data)) id))

(defn remove-from-history [id]
  (swap! search-data assoc-in [:history] (remove #{id} (:history @search-data))))

;(defn veza [id]
;  (swap! search-data assoc-in [:veza] id))

(defn sel-data [doc]
  (GET "/jus/search-data" {:params        {:doc doc}
                           :handler       (fn [x]
                                            (reset! search-data {:refresh false :parents (first x) :childs (second x) :selection doc :veza nil
                                                                 :search-type (:search-type @search-data) :graphics (:graphics @search-data) :history (:history @search-data)}))
                           :error-handler #(js/alert (str "error: " %))}))

(defn veza-data [doc]
  (GET "/jus/search-data" {:params        {:doc doc}
                           :handler       (fn [x] (swap! search-data assoc-in [:veza] {:sel doc :parents (first x) :childs (second x)}))
                           :error-handler #(js/alert (str "error: " %))}))

(defn select-doc-type []
  [rui/select-field {:id        "type"
                     :autoWidth true
                     ;:disabled  hide
                     :style     {;:width "20%"  :min-width "230px"
                                 :font-size   "12px" :display "inline-block" :padding-left "10px"
                                 :font-family "Roboto, sans-serif" :font-weight "bold"}
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

(defn ac2 [source]
  (let [source-filter (case (:search-type @search-data) 0 nil 1 #(= 1 (:type %)) 2 #(> (:type %) 1) 3 #(= 0 (:type %)))
        source-new (if source-filter (mapv #(dissoc % :type) (filterv source-filter source)) source)
        criteria (atom nil)]
    ;(println (first source-new))
    (fn [source]
      [:div
       [rui/auto-complete {:id                  "text"
                           :floating-label-text "Naziv"
                           :openOnFocus         true
                           :input-style         {:width "70%" :font-weight "bold"}
                           :style               {:display "inline-block" :width "75%" :margin-right "5%" :padding-left "10px"}
                           :dataSource          source-new
                           :maxSearchResults    50
                           :filter              (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
                           :full-width          true
                           ;:on-new-request      (fn [chosen index] (sel-data (:id (source-new index))))
                           :on-new-request      (fn [chosen index] (reset! criteria (:id (source-new index))))
                           :hint-text           "Unesi dio teksta iz naslova"
                           :list-style          {:height "250px" :width "300%"}}]
       (select-doc-type)
       (if @criteria [:div
                      [rui/flat-button {:label "Otvori" :primary true :on-click (fn [] (sel-data  @criteria))}]
                      [rui/flat-button {:label "Zapamti" :secondary true :on-click #(swap! search-data assoc-in [:history] (update-history @criteria))}]
                      [rui/flat-button {:label "Nova pretraga" :on-click (fn [e] (reset! show-ac (not @show-ac)) (reset! criteria nil))}]])])))

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

(defn label-text
  ( [label text width]
    (label-text label text width (:blue500 colors)))
  ( [label text width color]
    [:div {:style (merge {:width (str width "%") :display "inline-block" :font-family "Roboto, sans-serif" :font-size "15px" :color color}
                         (if (= "" label) {:font-weight "bold" :text-align "center"}))}
     [:span {:style {:font-weight "bold" :color (:cyan500 colors)}} label]
     text]))

(defn label-text-wide [label data]
   [:div {:style {:text-align   "center" :font-weight "bold" :margin-bottom "15px" :border-bottom-style "ridge"
                  :border-color (:cyan500 colors) :font-family "Roboto, sans-serif"}}
                  ;:color        (case (:Naredba data) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                  ;                                    (case (:Mandatory data) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow")}}
    (:title data)])

(defn yu-naredba-view [data]
  (let [width "50"]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "YU naredba/pravilnik" width (:red500 colors))
     (label-text "Službeni glasnik: " (:Glasnik data) width)]))

(defn bh-naredba-view [data]
  (let [width "25"]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     ;[:div {:style {:font-weight "bold" :margin-bottom "8px" :margin-top "8px" }} (:title data)]
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "BiH naredba" width (:blue500 colors))
     (label-text "Službeni glasnik: " (:Glasnik data) width)
     (label-text "Evropska direktiva: " [:a {:href (:Link-d data) :target "_blank"} (:Direktiva data)] width)
     [:a {:href (str "pdf/" (:Link-n data)) :target "_blank" :style {:font-weight "bold" :color (:cyan500 colors)}} "Prikaži dokument"]]))

(defn jus-view [data]
  (let [width "14" wide "20"
        color (case (:Mandatory data) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors))]
    [:div {:style {:padding-left "2px" :margin-top "40px"}}
     (label-text-wide "Naziv: " data)
     (label-text "" (str (:name data) ":" (:JUSgodina data)) width)
     (label-text "Vrsta: " "JUS standard" width color)
     (label-text "Godina: " (:JUSgodina data) width)
     (label-text "Primjena: " (case (:Mandatory data) 0 "Obavezna" 1 "Djelimično obavezna" 2 "Za upotrebu") wide)
     (label-text "Broj strana: " (:Strana data) width)
     (label-text "ICS: " (:ICS data) width)]))

(defn check-veza [id]
  (if (some #{id} (clojure.set/union (first (:childs @search-data)) (first (:parents @search-data)))) true false))

(defn connected-docs [docs header label]
  (let [history (if (:JUSId (first docs))  false true)
        docs (if history docs  (sort-by (juxt #(case (:Naredba %) 1 1 2 2 3 3 0 4) :JUSId) < docs))
        color (fn [doc] (case (:Naredba doc) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                                             (case (:Mandatory doc) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow"))]
   [rui/table {:selectable    false :height (str (- (if (< (count docs) 5) (* (count docs) 48) 240) 0) "px")
               :on-cell-click (fn [row coll] (if (= coll 1) (sel-data (or (:JUSId (nth docs row)) (:name (nth docs row))))))
               :header-style  (if (= header "") {:margin-top "0px"} {:margin-top "40px"})}
    (if-not (= header "")
      [rui/table-header {:display-select-all false :enable-select-all false}
       [rui/table-row {:style {:background-color (:cyan500 colors)}}
        (if-not (= label "")
          [rui/table-header-column {:style { :width "7%"  :font-size "14px" :text-align "left" :color "black" :font-family "Roboto, sans-serif"}}  label])
        [rui/table-header-column {:style { :max-width "0" :white-space "nowrap" :overflow "hidden" :text-overflow "ellipsis" :font-family "Roboto, sans-serif"
                                          :width     (if (= label "") "92%" "100%") :font-size "14px" :text-align "center" :color "white" :text-transform "uppercase"}}  header]
        (if-not history
          [rui/table-header-column {:style {:width "7%"}}
           [rui/icon-button {:tooltip    "Brisi vezu" :on-click #(swap! search-data assoc-in [:veza] {})
                             :tooltip-styles   {:margin-top "-35px" :width "80px" :right "40px"}
                             :style      {:vertical-align "top" :float "right" :margin-top "0px"}
                             :icon-style {:width "24px" :height "24px" :color "white"} :tooltip-position "bottom-left"} (ic/content-clear)]])]])
    [rui/table-body {:display-row-checkbox false :pre-scan-rows false :show-row-hover true}
     (doall (for [doc docs]
             (let [check-veza (if-not history true (check-veza (or (:JUSId doc) (:name doc))))]
              [rui/table-row {:key (or (:JUSId doc) (:name doc))  :style {:font-weight "normal" :color (color doc)}}
               [rui/table-row-column {:style {:width "85%" :cursor "pointer"}}
                [:span {:style {:font-weight "bold"}}
                 (if (= 0 (:Naredba doc)) (str (or (:JUSId doc) (:name doc)) ":" (:JUSgodina doc) "  "))]
                (or (:JUSopis doc) (:title doc))]
               (if  check-veza
                 [rui/table-row-column {:style {:width "7%"}}
                  [rui/icon-button {:tooltip          "Prikaži vezu"
                                    :tooltip-position "top-left"
                                    :tooltip-styles   {:margin-top "30px" :width "70px" :right "10px"}
                                    :on-click         #(veza-data (or (:JUSId doc) (:name doc)))
                                    :style            {:width "24px" :height "24px" :float "right"}
                                    :icon-style       {:width "20px" :height "20px" :color (:cyan500 colors)}} (ic/social-share)]]
                 [rui/table-row-column {:style {:width "7%"}}])
               (if history
                 [rui/table-row-column {:style {:width "7%"}}
                  [rui/icon-button {:tooltip          "Brisi iz zapamćenih"
                                    :tooltip-position "top-left"
                                    :tooltip-styles   {:margin-top "30px" :width "70px" :right "10px"}
                                    :on-click         #(remove-from-history (:name doc))
                                    :style            {:width "24px" :height "24px" :float "right"}
                                    :icon-style       {:width "20px" :height "20px" :color (:cyan500 colors)}} (ic/content-clear)]])])))]]))

(defn history [header]
  (let [list (take 10 (:history @search-data))
        history-data (fn [x] (first (filter #(= x (:name %)) @db-tree)))]
    (if (> (count list) 0)(connected-docs (map #(history-data %) list) header ""))))

(defn search-result [pretraga]
  (let [search @search-data
        result (first (filter #(= (:selection search) (:name %)) @db-tree))
        result-veza (first (filter #(= (:sel (:veza search)) (:name %)) @db-tree))
        childs (second (:childs search))
        parents (second (:parents search))
        veza-childs (second (:childs (:veza search)))
        veza-parents (second (:parents (:veza search)))
        type (:Naredba result)]
    [:div {:class "foo" :style {:font-size "16px" :font-family "Roboto, sans-serif" :margin-bottom "20px"} :key "sr"}
         [rui/icon-button {:tooltip    "Brisi pretragu" :on-click #(clear-criteria true)
                           :style      {:vertical-align "top" :float "right" :margin-top "-40px"}
                           :icon-style {:width "24px" :height "24px" :color (:cyan500 colors)} :tooltip-position "bottom-left"} (ic/content-clear)]
         (case type 0 (jus-view result ) 1 (bh-naredba-view result) (yu-naredba-view result))
         (if-not (:sel (:veza search))
           [rui/tabs {:style {:margin-top "40px"}}
            [rui/tab {:label "Vezan za dokumente:"}
             (connected-docs parents "" "")]
            [rui/tab {:label "Vezani dokumenti:"}
             (connected-docs childs "" "")]
            [rui/tab {:label "Zapamćeni dokumenti"}
             (history "")]]
           ;[rui/paper {:z-depth 2 :class-name "col-md-12" :style {:margin-top "40px" :margin-bottom "20px"}}
           (connected-docs veza-parents (if (= (:Naredba result-veza) 0) (str (str (:name result-veza) ":" (:JUSgodina result-veza)) " " (:title result-veza)) (:title result-veza)) "Veza sa:"))
         ;[:hr]
         pretraga]))

(def logo  (r/as-element [rui/svg-icon {:color "white" :view-box "0 0 100 50" :style {:width "100px" :height "50px"}}
                             [:text {:x 10 :y 35 :font-size 35 :style {:fill "rgb(255, 64, 129)"} } "eJUS"]]))

(def tree-icon  (r/as-element [rui/svg-icon {:color "black" :view-box "0 0 36 36" :style {:width "36px" :height "36px"}}
                               [:path {:d "M30.5 24h-0.5v-6.5c0-1.93-1.57-3.5-3.5-3.5h-8.5v-4h0.5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h0.5v4h-8.5c-1.93 0-3.5 1.57-3.5 3.5v6.5h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-0.5v-6h8v6h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-0.5v-6h8v6h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5zM6 30h-4v-4h4v4zM18 30h-4v-4h4v4zM14 8v-4h4v4h-4zM30 30h-4v-4h4v4z"}]]))

;"M17 0l-3 3 3 3-7 8h-7l5.5 5.5-8.5 11.269v1.231h1.231l11.269-8.5 5.5 5.5v-7l8-7 3 3 3-3-15-15zM14 17l-2-2 7-7 2 2-7 7z"

(defn home-page [source source-text]
  (let [search-d @search-data
        show @show-ac
        pretraga [:div {:class "foo" :key "rbg" :style {:margin-top (if-not (:selection search-d)"20px" "60px")}}
                  [:div {:style {:font-size "14px" :height "48px" :padding-top "5px" :display "flex" :align-items "center" :justify-content "center"  :background-color (:cyan500 colors) :color "white"}}
                   (if-not (:selection search-d)
                    "Pretraga podataka o naredbama/pravilnicima/standardima"
                     "Pretraga vezanih dokmenata")]
                  [:div {:style {:padding "3px 0px 0px 0px" :margin-top "10px" :border-style "ridge" :border-radius "10px" :border-color (:cyan500 colors)}}
                    [:div (if show [:div {} [ac2 source-text]] [ac2 source-text])]]
                  (if-not (:selection search-d)[:div (history "Zapamćeni dokumenti")])]]
    [rui/mui-theme-provider {:mui-theme (ui/get-mui-theme {:palette {:text-color (:blue500 colors)}})}
     [:div
      [rui/app-bar {:title              "Veze JUS standarda i harmoniziranih BiH naredbi"
                    :title-style        {:text-align "center"}
                    :icon-element-left  logo
                    :icon-style-left    {:width "100px"}
                    :showMenuIconButton true
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
          pretraga
          (search-result pretraga))]]
      [rui/paper {:class-name "col-md-12" :z-depth 4 :style {:margin-top "20px" :position "absolute" :top "580px" :max-width "98%" :display (if-not (:graphics search-d) "none")}} ;}}
       [rui/icon-button {:tooltip          "Zatvori"
                         :tooltip-position "bottom-left"
                         :on-click         #(swap! search-data assoc-in [:graphics] false)
                         :style            {:vertical-align "top" :float "right"}
                         :icon-style       {:color (:cyan500 colors)}} (ic/content-clear)]
       [:div {:class-name "col-md-8" :style {:font-size "20px" :margin-top "12px" :display "inline-block"}} "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
        [:div {:id "app" :style {:max-height "500px" :overflow "auto"}}]]
       [:div {:class-name "col-md-3" :style {:font-size "20px" :display "inline-block"}} (legend)]]]]))

(defn mount-root []
  (r/render
    [home-page (mapv :name @db-tree) (mapv #(hash-map :text (if (= (:Naredba %) 0) (str (:name %) " " (:title %)) (:title %))
                                                      :value  (ui/menu-item {:style {:color (case (:Naredba %) 1 (:blue500 colors) 2 (:red500 colors) 3 (:red500 colors) 0
                                                                                                                 (case (:Mandatory %) 2 (:lightgrey colors) 1 (:grey colors) 0 (:darkgrey colors)) "yellow")}
                                                                             :primary-text (if (= (:Naredba %) 0) (str (:name %) " " (:title %)) (:title %))})
                                                      :id (:name %)
                                                      :type (:Naredba %))
                                           @db-tree)]
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

