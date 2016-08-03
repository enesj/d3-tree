(ns foo.example.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :as m :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST json-response-format json-request-format url-request-format ajax-request]]
            [foo.example.flare :as flare]
            [cljs.core.async :refer [chan close! timeout]]
            [cljsjs.d3]))

(enable-console-print!)

(def db-tree (atom nil))
(def scroll-data (atom nil))
(def search-data (atom {:selection nil :childs nil :parents nil :refresh false}))
(def click-delay (atom 0))

(add-watch search-data :update-tree
           (fn [key atom old-state new-state]
             (if (= false (:refresh @search-data))
               (doseq [doc (merge (first (:parents old-state)) (:selection old-state))]
                 (.. js/d3
                     (selectAll "textPath")
                     (filter (fn [d i] (if (= (.-name d) doc) (js* "this") nil)))
                     (style "fill" "black"))))
             (if (not-empty (first (:parents new-state)))
             (doseq [doc (merge (first (:parents new-state)) (:selection new-state))]
               (.. js/d3
                   (selectAll "textPath")
                   (filter (fn [d i] (if (= (.-name d) doc) (js* "this") nil)))
                   (style "fill" "orange")
                   )))))

(def margin {:top 30, :right 20, :bottom 30, :left 30})
(def width (- 800 (:left margin) (:right margin)))
(def barHeight 20)
(def barWidth (* width .7))
(def duration 400)
(def title-lenght 140)
(def y-chars-ratio 5)

(defn all-childs [parent]
  (GET "/jus/childs" {:params        {:parent parent}
                      :handler       (fn [x] (swap! search-data assoc-in [:childs] x))
                      :error-handler #(js/alert (str "error: " %))}))

(defn all-parents [child]
  (GET "/jus/parents" {:params        {:child child}
                       :handler       (fn [x] (swap! search-data assoc-in [:parents] x))
                       :error-handler #(js/alert (str "error: " %))}))




(defn search-relations [doc]
  (all-parents doc)
  (all-childs doc)
  (swap! search-data assoc-in [:selection] doc))


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


(defn sel-data [doc]
  (GET "/jus/search-data" {:params        {:doc doc}
                           :handler       (fn [x]
                                            (swap! search-data assoc-in [:parents] (first x))
                                            (swap! search-data assoc-in [:childs] (second x))
                                            (swap! search-data assoc-in [:selection] doc))
                           :error-handler #(js/alert (str "error: " %))}))


(defonce svg
         (.. js/d3
             (select "#app")
             (append "svg:svg")
             (attr "width" (+ width (:left margin) (:right margin)))
             (append "svg:g")
             (attr "transform" (str "translate(" (:left margin) "," (:top margin) ")"))))

(defn collapse [d]
  ;(js/console.log d)
  (let [children (.-children d)]
    (if children (do (set! (.-_children d) (.-children d))
                     (set! (.-children d) nil)
                     (mapv #(collapse %) (.-_children d))))))

(defn expand-first-level [d]
  (set! (.-children d) (.-_children d)) (set! (.-_children d) nil))


(def data-flare (clj->js []))

(defn click-fn [d d3-tree ctrl]
  (if (.-children d) (do (set! (.-_children d) (.-children d)) (set! (.-children d) nil))
                     (do (if (.-_children d)
                           (do (set! (.-children d) (.-_children d)) (set! (.-_children d) nil))
                           (set! (.-children d) (clj->js (:children (first (filter #(= (:name %) (.-name d)) @db-tree)))))
                           )))

  (d3-tree d)
  (if ctrl (do (sel-data (.-name d)) (swap! search-data assoc-in [:refresh] false)) (swap! search-data assoc-in [:refresh] true))
  )


(defn d3-tree [data-new]
  (let [tree-nodes (.. tree
                       (nodes data-flare))

        parents (first (:parents @search-data))

        height (+ (* (.-length tree-nodes) barHeight) (:top margin) (:bottom margin))

        node (.. svg
                 (selectAll "g.node")
                 (data tree-nodes (fn [d i]
                                    (or (.-id d) (set! (.-id d) (str (clojure.string/replace (.-name d) "." "") (rand-int 1000)))))
                       ))

        link (.. svg
                 (selectAll "path.link")
                 (data (.. tree (links tree-nodes)) (fn [d] (.-id (.-target d)))))


        node-group (.. node
                       enter
                       (append "svg:g")
                       (attr "class" "node")
                       (attr "transform" (fn [d i] (str "translate(" (.-y0 data-new) "," (.-x0 data-new) ")")))
                       (style "opacity" 1)
                       )]

    (.. js/d3 (select "svg")
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
        ;(style "stroke" "lightgray")
        (style "fill" (fn [d i]
                        (case (.-type d) 1 "blue" 2 "red" 3 "red" 0 (case (.-mandatory d) 2 "#cccccc" 1 "#666666" 0 "black") "yellow"))))

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
        (attr "d" (fn [d] (if (= (.-shorttitle d) "") "m7 2 1000 0" "m100 5 1000 0")))
        ;(attr "stroke" "black")
        )

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
                  (.-title d))))
        ;(each (fn [d i] (doseq [title (for [title-part (partition 2 1 (range 0 500 50) )]
        ;                       (.substring (.-title d) (first title-part) (second title-part)) )]
        ;          (.. js/d3
        ;              (select (js* "this"))
        ;              (append "tspan")
        ;              (text title)
        ;              (attr "x" 0)
        ;              (attr "dy" 15)))))
        )


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

    (.. link
        exit
        transition
        (duration duration)
        (attr "d" (fn [d] (diagonal (clj->js {:source {:x (.-x0 data-new) :y (.-y0 data-new)} :target {:x (.-x0 data-new) :y (.-y0 data-new)}}))))
        remove)

    (.. tree-nodes (forEach (fn [d] (set! (.-x0 d) (.-x d)) (set! (.-y0 d) (.-y d)))))))


(defn test-chars []

  (GET "/jus/active-data" {:handler       (fn [x]
                                     (reset! db-tree x))
                    :error-handler #(js/alert (str "error: " %))}))

(defn init-veza []
  (GET "/jus/tree" {:handler       (fn [x]
                                     (reset! db-tree x)
                                     (set! data-flare (clj->js (first (filter #(= (:name %) "1000") @db-tree))))
                                     (d3-tree data-flare)
                                     ;(collapse data-flare)
                                     ;(expand-first-level data-flare)
                                     ;(d3-tree data-flare)
                                     )
                    :error-handler #(js/alert (str "error: " %))}))

(defn main []
  (init-veza))
