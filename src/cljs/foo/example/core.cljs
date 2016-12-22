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
(def search-data (atom {:selection   nil :graph-selection nil :veza {:sel nil :childs nil :parents nil} :childs nil :parents nil :refresh true
                        :search-type 0 :graphics false :history [] :search-text ""}))

(def candidate (atom {:id nil :veze 0}))

(def margin {:top 24, :right 20, :bottom 30, :left 30})
(def width (- 800 (:left margin) (:right margin)))
(def barHeight 17)
(def barWidth (* width .7))
(def duration 400)
(def title-lenght 140)
(def y-chars-ratio 5)
(def history-size 20)

(def colors
  {:darkblue  (ui/color :blue900)
   :blue      (ui/color :blue500)
   :red       (ui/color :red500)
   :lightgrey (ui/color :grey300)
   :grey      (ui/color :grey600)
   :darkgrey  (ui/color :grey900)
   :cyan      (ui/color :cyan500)
   :bh        (ui/color :blue500)
   :yu        (ui/color :red500)
   :jus1      (ui/color :grey900)
   :jus2      (ui/color :grey600)
   :jus3      (ui/color :grey300)
   :all       (ui/color :yellow500)})

(defn doc-colors [type mandatory]
  (case type 1 (:bh colors) 2 (:yu colors) 3 (:yu colors) 0
             (case mandatory 2 (:jus3 colors) 1 (:jus2 colors) 0 (:jus1 colors)) (:all colors)))


(defn check-veza [id]
  (let [search-d @search-data
        veza-type (cond
                    (some #{id} (first (:childs search-d))) 0
                    (some #{id} (first (:parents search-d))) 1
                    :else false)]
    veza-type))

(defn count-veze-history [search-d id veza-type index]
  (if veza-type
    (GET "/jus/search-data" {:params        {:doc id :verbose "0"}
                             :handler       (fn [x] (swap! search-data assoc-in [:history index :veze] (count (if (= veza-type 0) (clojure.set/intersection (ffirst x) (first (:childs search-d)))
                                                                                                                                  (clojure.set/intersection (first (second x)) (first (:parents search-d)))))))
                             :error-handler #(js/alert (str "error: " %))})))

(defn veze-history []
  (swap!  search-data update-in [:history]  (fn [x] (mapv #(assoc-in % [:veze] 0) x)))
  (let [search-d @search-data
        indexed (map-indexed #(hash-map :index %1 :id (:id %2))  (take history-size (:history search-d)))]
    (doseq [item indexed]
           (count-veze-history search-d (:id  item) (check-veza (:id  item)) (:index item)))))

(defn sel-data [doc]
  (let [current-data @search-data]
    (GET "/jus/search-data" {:params        {:doc doc :verbose "1"}
                             :handler       (fn [x]
                                              (reset! search-data {:refresh     false :parents (first x) :childs (second x) :selection doc :veza nil :search-text (:search-text current-data)
                                                                   :search-type (:search-type current-data) :graphics (:graphics current-data) :history (:history current-data)}))

                             :error-handler #(js/alert (str "error: " %))})))


(defn get-doc-data [criteria db]
  (first (filter #(= (:name %) criteria) db)))

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
                     (style "text-decoration" "none"))))
             (if (not-empty (first (:parents new-state)))
               (doseq [doc (merge (first (:parents new-state)) (:selection new-state))]
                 (.. js/d3
                     (selectAll "textPath")
                     (filter (fn [d i] (if (= (.-name d) doc) (js* "this") nil)))
                     (style "text-decoration" "underline"))))))

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
                           (set! (.-children d) (clj->js (:children (get-doc-data (.-name d) @db-tree)))))))


  (d3-tree d)
  (if ctrl
    (do (sel-data (.-name d)) (js/console.log "ctrl"))
    (swap! search-data assoc-in [:graph-selection] [(.-name d)(.-title d) (.-type d) (.-mandatory d)]))
  (swap! search-data assoc-in [:refresh] true))


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
        (style "fill" (fn [d i] (doc-colors (.-type d) (.-mandatory d)))))
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
                    (if (and (:children (get-doc-data (.-name d) @db-tree)) (not (.-children d)))
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
                    (if (and (:children (get-doc-data (.-name d) @db-tree)) (not (.-children d)))
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

(defn clear-criteria []
  (let [current-data @search-data]
    (reset! search-data {:selection   nil :veza nil :childs nil :parents nil :refresh false :search-text ""
                         :search-type (:search-type current-data) :graphics (:graphics current-data) :history (:history current-data)})
    (reset! candidate {:id nil :veze 0})))

(defn update-history [id]
  (vec (conj (remove #(= id (:id %)) (:history @search-data)) {:id id :veze 0})))


(defn remove-from-history [id]
  (swap! search-data assoc-in [:history] (remove #(= id (:id %)) (:history @search-data))))


(defn veza-data [doc type]
  (let [search-d @search-data]
    (GET "/jus/search-data" {:params        {:doc doc :verbose "0"}
                             :handler       (fn [x] (swap! search-data assoc-in [:veza] {:sel doc :parents (first x) :childs (second x)
                                                                                         :path (if (= type 0) (clojure.set/intersection (ffirst x) (first (:childs search-d)))
                                                                                                              (clojure.set/intersection (first (second x)) (first (:parents search-d))))}))
                             :error-handler #(js/alert (str "error: " %))})))


(def legend-data
  {:default {:width  10
             :height 10
             :rx     2
             :ry     2}
   :items   [{:id "bih" :type :rect :x 0 :y 50 :fill (:bh colors)}
             {:id "bih-t" :type :text :x 20 :y 60 :text "BiH naredbe"}
             {:id "yu" :type :rect :x 120 :y 50 :fill (:yu colors)}
             {:id "yu-t" :type :text :x 140 :y 60 :text "YU naredbe"}
             {:id "jus1" :type :rect :x 0 :y 80 :fill (:jus1 colors)}
             {:id "jus1-t" :type :text :x 20 :y 90 :text "JUS sa obaveznom primjenom"}
             {:id "jus2" :type :rect :x 0 :y 100 :fill (:jus2 colors)}
             {:id "jus2-t" :type :text :x 20 :y 110 :text "JUS sa djelimično obaveznom primjenom"}
             {:id "jus3" :type :rect :x 0 :y 120 :fill (:jus3 colors)}
             {:id "jus3-t" :type :text :x 20 :y 130 :text "JUS za upotrebu"}
             ;{:id "sel" :type :rect :x 0  :y 120 :fill "white" :stroke "black"}
             {:id "sel-t" :type :text :x 0 :y 160 :text "Naredbe\\standardi koji sadrže rezultat pretrage" :text-decoration "underline"}]})

(defn legend []
  [:svg {:style    {:width "100%" :max-height "200px" :font-size "14px" :padding-left "15px" :padding-top "5px" :margin-top "12px" :border-style "ridge" :border-radius "10px" :border-color (:cyan colors)}
         :view-box [0 0 330 180]}
   [:g
    [:text {:x 0 :y 20 :font-size "20px" :fill (:blue colors)} "Legenda"]
    (for [item (:items legend-data)]
      ^{:key (:id item)} [(:type item) (merge (:default legend-data) (dissoc item :text :type))
                          (if (= (:type item) :text) (:text item))])]])

(defn  legend-data-h [bdg-y]
    {:default {:width  10
               :height 10
               :rx     2
               :ry     2}
     :colors  [{:id "bih" :type :rect :x 10 :y 1 :fill (:bh colors)}
               {:id "yu" :type :rect :x 22 :y 1 :fill (:yu colors)}
               {:id "jus1" :type :rect :x 34 :y 1 :fill (:jus1 colors)}
               {:id "jus2" :type :rect :x 46 :y 1 :fill (:jus2 colors)}
               {:id "jus3" :type :rect :x 58 :y 1 :fill (:jus3 colors)}
               {:id "doc-type" :type :text :x 80 :y 10 :text ""}]
     :badges  [{:id "badges-line" :type :line :x1 5 :y1 (+ bdg-y 15) :x2 445 :y2 (+ bdg-y 15) :stroke (:grey colors)}
               {:id "badge-text" :type :text :x 80 :y (+ bdg-y 30) :fill (:bh colors)  :text "Broj dokumenata u grupi"}
               {:id "badge-red" :type :circle :cx 38 :cy (+ bdg-y 26) :r 8 :fill "rgb(255, 64, 129)"}
               {:id "badge-red-label" :type :text :x 36 :y (+ bdg-y 30) :text "3" :fill "white" :font-size "10px"}
               {:id "badge-blue" :type :circle :cx 18 :cy (+ bdg-y 26) :r 8 :fill "rgb(0, 188, 212)"}
               {:id "badge-blue-label" :type :text :x 12 :y (+ bdg-y 30) :text "25" :fill "white" :font-size "10px"}]
     :buttons [{:id "buttons-line" :type :line :x1 5 :y1 15 :x2 430 :y2 15 :stroke (:lightgrey colors)}
               {:id "otvori" :type :text :x 9 :y 30  :fill "rgb(0, 188, 212)" :text "Otvori"}
               {:id "zapamti" :type :text :x 110 :y 30 :fill "rgb(255, 64, 129)" :text "Zapamti"}
               {:id "veze" :type :text :x 200 :y 30 :fill "rgb(255, 64, 129)" :text "Veze"}
               {:id "nova" :type :text :x 300 :y 30 :fill (:cyan colors) :text "Nova pretraga"}
               {:id "button-type" :type :text :x 50 :y 50 :fill (:bh colors)}]})

(defn legend-h []
  (let [legend-type (atom nil)
        change-type (fn [x] (do (js/console.log x) (reset! legend-type x)))]
    (fn []
     (let [search-d  @search-data
           show-badges (or (:selection search-d) (> (count (:history search-d) ) 0))
           legend-data-h (legend-data-h 2)
           legend-data (remove nil? (flatten (into (:colors legend-data-h) [(if show-badges (:badges legend-data-h))])))
           svg-heght (str (+ 16 (if show-badges 26 0) ) "px")]
      [:svg {:style {:width "450px" :height svg-heght :float "right" :font-size "12px" :background-color "white"  :padding-top "2px"  :margin-top "-15px"
                     :box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}};}};}}
       [:g
        ;[:text {:x 0 :y 10 :fill (:darkblue colors) :font-weight "bold" } "Legenda:  "]
        (doall (for [item legend-data]
                 ^{:key (:id item)} [(:type item) (merge (:default legend-data-h) (dissoc item :text :type) {:on-mouse-over #(change-type (:id item))
                                                                                                             :on-mouse-leave  #(change-type "")}
                                                         (if (= (:id item) "doc-type")
                                                           {:fill
                                                            (case @legend-type
                                                              "bih" (:bh colors)
                                                              "yu" (:yu colors)
                                                              "jus1" (:jus1 colors)
                                                              "jus2" (:jus2 colors)
                                                              "jus3" (:jus3 colors)
                                                              "sel-t" "black"
                                                              (:bh colors))}))
                                     (if (= (:id item) "doc-type") (case @legend-type
                                                                     "bih" "BiH Naredbe"
                                                                     "yu" "YU Naredbe\\Pravilnici"
                                                                     "jus1" "JUS sa obaveznom primjenom"
                                                                     "jus2" "JUS sa djelimično obaveznom primjenom"
                                                                     "jus3" "JUS za upotrebu"
                                                                     "sel-t" "Naredbe\\standardi koji sadrže rezultat pretrage"
                                                                     "Za objašnjenje pređi mišem preko odgovarajuće boje")
                                                                   (if (= (:id item) "badge-text") (case @legend-type
                                                                                                     "badge-blue-label" "Primjer 1: u grupi se nalazi 25 dokumenata"
                                                                                                     "badge-red-label" "Primjer 2: u grupi se nalazi 3 dokumenta"
                                                                                                     "Broj dokumenata u grupi")
                                                                                                 (:text item)))]))]]))))


(defn label-text
  ([label text width]
   (label-text label text width (:blue colors)))
  ([label text width color]
   [:div {:style (merge {:width (str width "%") :display "inline-block" :font-family "Roboto, sans-serif" :font-size "15px" :color color}
                        (if (= "" label) {:font-weight "bold" :text-align "center"}))}
    [:span {:style {:font-weight "bold" :color (:cyan colors)}} label]
    text]))

(defn label-text-wide [label data]
  [:div {:style {:text-align   "center" :font-weight "bold" :margin-bottom "15px" :border-bottom-style "ridge"
                 :border-color (:cyan colors) :font-family "Roboto, sans-serif"}}
   (:title data)])

(defn yu-naredba-view [data]
  (let [width "33"]
    [:div {:style {:padding-left "2px" :margin-top "30px"}}
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "YU naredba/pravilnik" width (:red colors))
     (label-text "Službeni glasnik: " (:Glasnik data) width)
     [:a {:href (str "pdf/" (:Link-n data)) :target "_blank" :style {:font-weight "bold" :color (:cyan colors)}} "Prikaži dokument"]]))

(defn bh-naredba-view [data]
  (let [width "25"]
    [:div {:style {:padding-left "2px" :margin-top "30px"}}
     ;[:div {:style {:font-weight "bold" :margin-bottom "8px" :margin-top "8px" }} (:title data)]
     (label-text-wide "Naziv: " data)
     (label-text "Vrsta: " "BiH naredba" width (:blue colors))
     (label-text "Službeni glasnik: " (:Glasnik data) width)
     (label-text "Evropska direktiva: " [:a {:href (:Link-d data) :target "_blank"} (:Direktiva data)] width)
     [:a {:href (str "pdf/" (:Link-n data)) :target "_blank" :style {:font-weight "bold" :color (:cyan colors)}} "Prikaži dokument"]]))

(defn jus-view [data]
  (let [width "14" wide "20"
        color (doc-colors (:Naredba data) (:Mandatory data))]
    [:div {:style {:padding-left "2px" :margin-top "30px"}}
     (label-text-wide "Naziv: " data)
     (label-text "" (str (:name data) ":" (:JUSgodina data)) width)
     (label-text "Vrsta: " "JUS standard" width color)
     (label-text "Godina: " (:JUSgodina data) width)
     (label-text "Primjena: " (case (:Mandatory data) 0 "Obavezna" 1 "Djelimično obavezna" 2 "Za upotrebu") wide)
     (label-text "Broj strana: " (:Strana data) width)
     (label-text "ICS: " (:ICS data) width)]))

(defn select-doc-type []
  [rui/select-field {:id        "type"
                     ;:autoWidth true
                     :style     {:padding-left "10px" :width "200px" :height "50px"}
                     :value     (:search-type @search-data)
                     :on-change (fn [event index value]
                                  (swap! search-data assoc-in [:search-text] "")
                                  (reset! candidate {:id nil :veze 0})
                                  (select-type-change value))}
   [rui/menu-item {:value 0 :primary-text "Svi dokumenti" :style {:border-left "10px solid rgba(255,235,59,1)"}}]
   [rui/menu-item {:value 1 :primary-text "BiH naredbe" :style {:border-left "10px solid rgba(33,150,243,1)"}}]
   [rui/menu-item {:value 2 :primary-text "YU naredbe/pravilnici" :style {:border-left "10px solid rgba(255,0,0,1)"}}]
   [rui/menu-item {:value 3 :primary-text "JUS standardi" :style {:border-left "10px solid rgba(190,190,190,1)"}}]])




(defn count-veze [id veza-type]
  (if veza-type
    (GET "/jus/search-data" {:params        {:doc id :verbose "0"}
                             :handler       (fn [x] (swap! candidate assoc-in [:veze] (count (if (= veza-type 0) (clojure.set/intersection (ffirst x) (first (:childs @search-data)))
                                                                                               (clojure.set/intersection (first (second x)) (first (:parents @search-data)))))))
                             :error-handler #(js/alert (str "error: " %))})))




(defn ac-search [source]
  (let [hover (atom "")
        message {:1 ["12px" "Prikazi podatke o dokumentu"]
                 :2 ["98px" "Zapamti ovaj dokument"]
                 :3 ["206px"  "Prikazi dokumente koji vezuju ovaj dokument sa izabranim dokumentom"]
                 :4 [(if (:selection @search-data)  "283px" "193px") "Obrisi kriterij za pretragu"]}]
    (fn [source]
      (let [search-d @search-data
            ref-criteria @candidate
            text (:search-text search-d)
            source-filter (case (:search-type search-d) 0 nil 1 #(= 1 (:type %)) 2 #(> (:type %) 1) 3 #(= 0 (:type %)))
            source-new (if source-filter (filterv source-filter source) source)]
        [:div
         [rui/auto-complete {:id                  "text"
                             :floating-label-text "Naziv"
                             :openOnFocus         true
                             :anchor-origin       (if (:selection search-d) {:vertical "top", :horizontal "left"} {:vertical "bottom", :horizontal "left"})
                             :target-origin       (if (:selection search-d) {:vertical "bottom", :horizontal "left"} {:vertical "top", :horizontal "left"})
                             :input-style         {:white-space "nowrap" :overflow "hidden" :text-overflow "ellipsis"}
                             :style               {:display "inline-block" :width "75%" :padding-left "10px"}
                             :dataSource          source-new
                             :maxSearchResults    50
                             :search-text         (:search-text search-d)
                             :filter              (aget js/MaterialUI "AutoComplete" "caseInsensitiveFilter")
                             :full-width          true
                             :on-new-request      (fn [chosen index]
                                                    (reset! candidate {:id (:id (source-new index)) :veze 0})
                                                    (count-veze (:id (source-new index)) (check-veza (:id (source-new index))))
                                                    (swap! search-data assoc-in [:search-text] (.-text chosen)))
                             :hint-text           "Unesi dio teksta iz naslova"
                             :list-style          {:max-height "250px" :width "300%"}}]
         (select-doc-type)
         (if (:id ref-criteria) [:div
                                 [rui/flat-button {:label "Otvori" :primary true
                                                   :on-click (fn [e] (swap! search-data assoc-in [:search-text] "") (reset! candidate {:id nil :veze 0}) (sel-data (:id ref-criteria)))
                                                   :on-mouse-over #(reset! hover :1)
                                                   :on-mouse-leave #(reset! hover "")}]
                                 [rui/flat-button {:label "Zapamti" :secondary true
                                                   :on-click (fn [e] (do (swap! search-data assoc-in [:history] (update-history (:id ref-criteria)))))
                                                   :on-mouse-over #(reset! hover :2)
                                                   :on-mouse-leave #(reset! hover "")}]
                                 (if (:selection search-d)
                                   [rui/flat-button {:label "Veze"  :secondary true
                                                     :on-click (fn [e] (swap! search-data assoc-in [:search-text] "") (reset! candidate {:id nil :veze 0}) (veza-data (:id ref-criteria) (check-veza (:id ref-criteria))))
                                                     :on-mouse-over #(reset! hover :3)
                                                     :on-mouse-leave #(reset! hover "")}
                                    [rui/badge {:badge-content (:veze ref-criteria)  :primary true :style {:margin-left "40px" :position "absolute" } :badge-style {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}]])
                                 [rui/flat-button {:label "Nova pretraga"
                                                   :on-click (fn [e] (reset! candidate {:id nil :veze 0}) (swap! search-data assoc-in [:search-text] "") (reset! hover ""))
                                                   :on-mouse-over #(reset! hover :4)
                                                   :on-mouse-leave #(reset! hover "")}]
                                 [:div {:style {:position "absolute" :margin-top "8px" :padding-left "3px" :padding-right "3px" :margin-left (first (message @hover))
                                                :background-color (:darkgrey colors) :color (:lightgrey colors) :border-radius "6px"}}
                                       (second (message @hover))]])]))))


                        ;[:div {:style {:height "5px"}}])])))

(defn docs-table [docs header label history]
  (let [docs (if history docs (sort-by (juxt #(case (:Naredba %) 1 1 2 2 3 3 0 4) :JUSId) < docs))
        color (fn [doc] (doc-colors (:Naredba doc) (:Mandatory doc)))
        s-data @search-data
        veza (:sel (:veza s-data))]
    [rui/table {:selectable    false
                :height (str (- (if (< (count docs) 5) (* (count docs) 48) 240) 0) "px")
                :on-cell-click (fn [row coll] (let [red (nth docs row)
                                                    id (or (:JUSId red) (:name red))
                                                    title (or (:title red) (:JUSopis red))
                                                    naziv (if (= (:Naredba red) 0) (str id " " title) title)]
                                                (if (= coll 1) (do (reset! candidate {:id id :veze 0})
                                                                   (count-veze id (check-veza id))
                                                                   (if (or history (:graphics s-data)) (sel-data id) (swap! search-data assoc-in [:search-text] naziv))))))
                :header-style  (if (= header "") {:margin-top "0px" } {:margin-top "20px"})
                :wrapper-style {:overflow "visible"}}
     (if-not (= header "")
       [rui/table-header {:display-select-all false :enable-select-all false :adjust-for-checkbox false}
        [rui/table-row {:style {:background-color (:cyan colors) :font-family "Roboto, sans-serif" :font-size "14px"} :selectable false}
         (if-not (= label "")
          [rui/table-header-column {:style {:width "68px"  :color "black" :font-weight "bold" :padding-left "1%" :padding-right "1%"}} label])
         [rui/table-header-column {:style {:overflow   "hidden" :text-overflow "ellipsis" :color "white"
                                           :text-align "center" :text-transform "uppercase" :padding-left "1%" :padding-right "1%"}}
          [rui/badge {:badge-content (count docs) :secondary true :style { :position "absolute" :margin-top "-15px" :margin-left "-15px" :padding-left "0%" :padding-right "0%"}
                      :badge-style {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}]
          header]
         (if veza
           [rui/table-header-column {:style {:width "7%"}}
            [rui/icon-button {:tooltip        "Brisi vezu"
                              :on-click #(do  (swap! search-data assoc-in [:search-text] "")(reset! candidate {:id nil :veze 0}) (swap! search-data assoc-in [:veza] {}))
                              :tooltip-styles {:margin-top "-35px" :width "80px" :right "40px"}
                              :style          {:vertical-align "top" :float "right" :margin-top "0px"}
                              :icon-style     {:width "24px" :height "24px" :color "white"} :tooltip-position "bottom-left"} (ic/content-clear)]])]])
     [rui/table-body {:display-row-checkbox false :pre-scan-rows false :show-row-hover false}
      (doall (map #(let [jusid (:JUSId %)
                         name (:name %)
                         id (or jusid name)
                         check-veza (or veza (if (and history (:selection s-data)) (check-veza id)))]
                     [rui/table-row {:key id :style {:cursor "pointer" :color (color %)}}
                      [rui/table-row-column
                       (if (= 0 (:Naredba %))
                         [:span {:style {:font-weight "bold"}} (str id ":" (:JUSgodina %) " ")])
                       (or (:JUSopis %) (:title %))]
                      (if (and history check-veza (:selection s-data))
                        [rui/table-row-column {:style {:width "7%"}}
                         [:div
                          [rui/icon-button {:tooltip          "Prikaži vezu"
                                            :tooltip-position "top-left"
                                            :tooltip-styles   {:margin-top "30px" :width "70px" :right "10px"}
                                            :on-click         (fn [x] (veza-data id check-veza))
                                            :style            {:width "24px" :height "24px" :float "right"}
                                            :icon-style       {:width     "20px" :height "20px" :color (:cyan colors)
                                                               :transform (if (= 1 check-veza) "rotate(0deg)" "rotate(180deg)")}}
                           (ic/social-share)]
                          (if history
                            [rui/badge {:badge-content (:veze (first (filter (fn [x] (= id (:id x))) (:history s-data))))
                                        :primary       true
                                        :style         {:margin-left "10%" :margin-top "-6px" :position "relative" :overflow "visible"}
                                        :badge-style   {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}])]]
                        [rui/table-row-column {:style {:width "7%"}}])
                      (if history
                        [rui/table-row-column {:style {:width "4%"}}
                         [rui/icon-button {:tooltip          "Brisi iz zapamćenih"
                                           :tooltip-position "top-left"
                                           :tooltip-styles   {:margin-top "30px" :width "100px" :right "10px"}
                                           :on-click         (fn [x] (remove-from-history name))
                                           :style            {:width "24px" :height "24px" :float "right"}
                                           :icon-style       {:width "20px" :height "20px" :color (:cyan colors)}} (ic/content-clear)]])])
                  docs))]]))

(defn history [history-list header]
  (let [history-data (fn [x] (get-doc-data x @db-tree))]
    (docs-table (map #(history-data (:id %)) history-list) header "" true)))

(defn ac-source [db]
  (let [color (fn [x] (doc-colors (:Naredba x) (:Mandatory x)))]
    (mapv #(let [jusid (:JUSId %)
                 name (:name %)
                 id (or jusid name)
                 jusopis (:JUSopis %)
                 title (:title %)
                 naredba-naziv (or jusopis title)
                 jus-naziv (str id " " naredba-naziv)
                 naziv (if (= (:Naredba %) 0) jus-naziv naredba-naziv)]
             (hash-map :text naziv
                       :value (ui/menu-item {:style        {:color (color %)}
                                             :primary-text naziv})
                       :id id
                       :type (:Naredba %)))
          db)))

(defn pretraga [db title]
  (let [search-d @search-data
        history-list (take history-size (:history search-d))]
    [:div {:class "foo" :key "rbg" :style {:padding-bottom "20px" :padding-top "20px" :margin-top (if-not (:selection search-d) "0px" "20px")}}
     [:div {:style {:font-size        "14px" :height "48px" :padding-top "5px" :display "flex" :align-items "center" :justify-content "center"
                    :background-color (:cyan colors) :color "white"}}
      title]
     ;(if-not (:selection search-d)
     ;  "Pretraga podataka o naredbama/pravilnicima/standardima"
     ;  "Pretraga vezanih dokmenata")]
     [:div {:style {:padding "3px 0px 0px 0px" :margin-top "10px" :margin-bottom "10px" :border-style "ridge" :border-radius "10px" :border-color (:cyan colors)}}
      [:div [ac-search db]]]
     (if-not (or (= (count history-list) 0) (:selection search-d)) (history history-list "Zapamćeni dokumenti"))]))

(defn graph-right []
  (let [graph-selection @search-data
        choice (first (:graph-selection graph-selection))
        title (second (:graph-selection graph-selection))
        type (nth (:graph-selection graph-selection) 2)
        mandatory (last (:graph-selection graph-selection))
        naziv (if (= type 0) (str choice " " title) title)]
    [:div {:style {:margin-top "15%" :color (doc-colors  type  mandatory)}}
     [:div naziv]
     [rui/flat-button {:label "Otvori" :primary true :on-click (fn [e]   (sel-data choice))}]
     [rui/flat-button {:label "Zapamti" :secondary true :on-click (fn [e] (do (swap! search-data assoc-in [:history] (update-history choice))))}]]))

(defn graph []
       [rui/paper {:class-name "col-md-12" :z-depth 4 :style {:margin-top "20px" :display (if-not (:graphics @search-data) "none")}} ;}}
        [rui/icon-button {:tooltip          "Zatvori"
                          :tooltip-position "bottom-left"
                          :on-click        (fn [] (swap! search-data assoc-in [:graph-selection] nil)(swap! search-data assoc-in [:graphics] false))
                          :style            {:vertical-align "top" :float "right"}
                          :icon-style       {:color (:cyan colors)}} (ic/content-clear)]
        [:div {:class-name "col-md-8" :style {:font-size "20px" :margin-top "12px" :display "inline-block"}} "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
         [:div {:id "app" :style {:max-height "500px" :overflow "auto"}}]]
        [:div {:class-name "col-md-4" :style {:font-size "14px" :display "inline-block" }} (graph-right)]])


(defn badges [count-p count-c count-h]
  [:div
    [rui/badge {:badge-content count-p  :secondary true :style {:margin-left "30%" :position "absolute" :margin-top "-50px"} :badge-style {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}]
    [rui/badge {:badge-content count-c :secondary true :style {:margin-left "63%" :position "absolute" :margin-top "-50px"} :badge-style {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}]
    [rui/badge {:badge-content count-h :secondary true :style {:margin-left "96%" :position "absolute" :margin-top "-50px"} :badge-style {:box-shadow "rgba(0, 0, 0, 0.156863) 0px 3px 10px, rgba(0, 0, 0, 0.227451) 0px 3px 10px"}}]])



(defn search-result []
  (let [tab-index (atom 0)]
    (fn []
      (let [search @search-data
            db @db-tree
            result (get-doc-data (:selection search) db)
            result-veza (get-doc-data (:sel (:veza search)) db)
            childs (second (:childs search))
            parents (second (:parents search))
            count-p (count (first (:parents search)))
            count-c (count (first (:childs search)))
            veza-path  (mapv #(get-doc-data % db) (:path (:veza search)))
            type (:Naredba result)
            history-list (take history-size (:history search))
            count-h (count history-list)
            badges [badges count-p count-c count-h]]
        [:div {:class "foo" :style {:padding-bottom "0px" :padding-top "0px" :font-size "16px" :font-family "Roboto, sans-serif"} :key "sr"}
         [rui/icon-button {:tooltip    "Brisi pretragu"
                           :on-click   #(clear-criteria)
                           :style      {:vertical-align "top" :float "right" :margin-top "-20px" :height "24px"}
                           :icon-style {:width "24px" :height "24px" :color (:cyan colors)} :tooltip-position "bottom-left"} (ic/content-clear)]
         (case type 0 (jus-view result) 1 (bh-naredba-view result) (yu-naredba-view result))
         (if (= (count veza-path ) 0)
           [:div
            [rui/tabs {:style {:margin-top "20px"} :on-change (fn [_ _ x]
                                                                (if (=  2 (.-index (.-props x))) (veze-history))
                                                                (swap! search-data assoc-in [:search-text] "")
                                                                (reset! candidate {:id nil :veze 0})
                                                                (reset! tab-index (.-index (.-props x))))}
             [rui/tab {:label "Vezan za dokumente:"}
              badges
              (docs-table parents "" "" false)]
             [rui/tab {:label "Vezani dokumenti:"}
              badges
              (docs-table childs "" "" false)]
             [rui/tab {:label "Zapamćeni dokumenti"}
              badges
              (history history-list "")]]
            (if-not (:graphics search)
              (case @tab-index
                0 (if (> count-p 0) (pretraga (ac-source parents) "Pretraga dokumenata koji vezuju"))
                1 (if (> count-c 0) (pretraga (ac-source childs) "Pretraga vezanih dokumenata"))
                2 (if (> count-h 0) (pretraga (ac-source (mapv #(get-doc-data % db) (take history-size (map :id (:history search))))) "Pretraga zapamćenih dokumenta"))
                [:div ""]))]
           [:div
            (docs-table veza-path (if (= (:Naredba result-veza) 0) (str (str (:name result-veza) ":" (:JUSgodina result-veza)) " " (:title result-veza)) (:title result-veza)) "Veza sa:" false)
            (pretraga (ac-source veza-path) "Pretraga zapamćenih dokumenta")])]))))


(def logo (r/as-element [rui/svg-icon {:color "white" :view-box "0 0 100 50" :style {:width "100px" :height "50px"}}
                         [:text {:x 10 :y 35 :font-size 35 :style {:fill "rgb(255, 64, 129)"}} "eJUS"]]))

(def tree-icon (r/as-element [rui/svg-icon {:color "black" :view-box "0 0 36 36" :style {:width "36px" :height "36px"}}
                              [:path {:d "M30.5 24h-0.5v-6.5c0-1.93-1.57-3.5-3.5-3.5h-8.5v-4h0.5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h0.5v4h-8.5c-1.93 0-3.5 1.57-3.5 3.5v6.5h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-0.5v-6h8v6h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5h-0.5v-6h8v6h-0.5c-0.825 0-1.5 0.675-1.5 1.5v5c0 0.825 0.675 1.5 1.5 1.5h5c0.825 0 1.5-0.675 1.5-1.5v-5c0-0.825-0.675-1.5-1.5-1.5zM6 30h-4v-4h4v4zM18 30h-4v-4h4v4zM14 8v-4h4v4h-4zM30 30h-4v-4h4v4z"}]]))

(defn home-page [db]
  (let [search-d @search-data]
    [rui/mui-theme-provider {:mui-theme (ui/get-mui-theme {:palette {:text-color (:blue colors)}})}

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
                    :style              {:background-color (:darkblue colors) :margin-bottom "20px"}}
       [:div {:class-name "col-md-12" :style {:width "98%" :position "absolute" :top "70px" :left 0}}
        [legend-h]]]
      [rui/paper {:z-depth 2 :class-name "col-md-12" :style {:margin-top "10px"}}
       [rui/css-transition-group {:transition-name          "example"
                                  :transition-enter-timeout 600
                                  :transition-leave-timeout 500}
        (if-not (:selection search-d)
          (pretraga db "Pretraga podataka o naredbama/pravilnicima/standardima")
          [search-result])]]
      [graph]]]))


(defn mount-root []
  (let [db @db-tree]
    (r/render
      [home-page (ac-source db)]
      (.getElementById js/document "search-app"))))

(defn init-veza []
  (GET "/jus/tree" {:handler       (fn [x]
                                     (reset! db-tree x)
                                     (mount-root)
                                     (set! data-flare (clj->js (get-doc-data "1000" @db-tree)))
                                     (mount-svg)
                                     (d3-tree data-flare))
                    ;(collapse data-flare)
                    ;(expand-first-level data-flare)
                    ;(d3-tree data-flare)
                    :error-handler #(js/alert (str "error: " %))}))

(defn main []
  (init-veza))

