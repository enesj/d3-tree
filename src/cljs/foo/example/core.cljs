(ns foo.example.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r :refer [atom]]
            [foo.example.flare :as flare]
            [clojure.data :as d]
            [cljsjs.d3]))

(enable-console-print!)



(def margin {:top 30, :right 20, :bottom 30, :left 100})
(def width (- 500 (:left margin) (:right margin)))
;(def height (- 3000 (:top margin) (:bottom margin)))
(def barHeight 20)
(def barWidth (* width .7))
(def duration 400)

;(def root flare/flare)
(def diagonal
  (.. js/d3
      -svg
      diagonal
      (projection (fn [d] (clj->js [(.-y d) (.-x d)])))
      ))

(def tree
  (.. js/d3 -layout tree
      (nodeSize (clj->js [0, 20]))
      ))


(defonce svg
  (.. js/d3
      (select "#app")
      (append "svg:svg")
      (attr "width" (fn [d] (+ width (:left margin) (:right margin))))
      ;(attr "height" height)
      (append "svg:g")
      (attr "transform" (str "translate(" (:left margin) "," (:top margin) ")"))))

(defn collapse [d]
      (let [children (.-children d)]
           (if children (do (set! (.-_children d) (.-children d))
                            (set! (.-children d) nil)
                            (mapv #(collapse %) (.-_children d))))))

(defn expand-first-level [d]
      (set! (.-children d) (.-_children d)) (set! (.-_children d) nil)   )

(def data-flare (clj->js flare/flare))


(defn click-fn [d d3-tree node]
      (if (.-children d) (do (set! (.-_children d) (.-children d)) (set! (.-children d) nil))
                         (do (set! (.-children d) (.-_children d)) (set! (.-_children d) nil)))
      ;(set! (.-children (.-parent d)) nil)
      (d3-tree d)
      )


(defn d3-tree [data-new]
      (let [tree-nodes (.. tree
                           (nodes data-flare))

            height (+ (* (.-length tree-nodes) barHeight) (:top margin) (:bottom margin))

            node (.. svg
                     (selectAll "g.node")
                     (data tree-nodes (fn [d i] (or (.-id d) (set! (.-id d) (inc i))))))

            link (.. svg
                     (selectAll "path.link")
                     (data (.. tree (links tree-nodes)) (fn [d] (.-id (.-target d)))))

            node-group (.. node
                           enter
                           (append "svg:g")
                           (attr "class" "node")
                           (attr "transform" (fn [d i] (str "translate(" (.-y0 data-new) "," (.-x0 data-new) ")")))
                           (style "opacity" 1)
                           )
            ]


           (.. js/d3 (select "svg")
               transition
               (duration duration)
               (attr "height" height))

           (.. tree-nodes (forEach (fn [d i] (set! (.-x d) (* i 10)))))

           (.. node-group
               (append "svg:circle")
               (attr "class" "node-dot")
               (attr "r" 3)
               )

           (.. node-group
               (append "rect")
               (attr "y" (- 5))
               (attr "height" 10)
               (attr "width" barWidth)
               (attr "opacity" 1e-6)
               (on "click" (fn [d] (click-fn d d3-tree node))))

           (.. node-group
               (append "svg:text")
               (attr "text-anchor" "start")
               (attr "dx" 7)
               (attr "dy" 3)
               (text (fn [d] (.-name d))))

           (.. node-group
               transition
               (duration duration)
               (attr "transform" (fn [d i] (str "translate(" (.-y d) "," (.-x d) ")")))
               (style "opacity" 1))

           (.. node
               transition
               (duration duration)
               (attr "transform" (fn [d i] (str "translate(" (.-y d) "," (.-x d) ")")))
               (style "opacity" 1))

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
               (attr "d" (fn [d] (diagonal (clj->js {:source {:x (.-x0 data-new) :y (.-y0 data-new)} :target {:x (.-x0 data-new) :y (.-y0 data-new)}}))))
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

           (.. tree-nodes (forEach (fn [d] (set! (.-x0 d) (.-x d)) (set! (.-y0 d) (.-y d)))))
           ))


(defn main []
      (d3-tree data-flare)
      (collapse data-flare)
      (expand-first-level data-flare)
      (d3-tree data-flare)
      )
