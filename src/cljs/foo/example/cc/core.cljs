(ns foo.example.cc.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]
            [schema.core :as s :include-macros true]
            [foo.example.helper :as h]
            [foo.example.cc.core-middlepanel :as mp]
            [foo.example.cc.left-panel :as lp]
            [foo.example.cc.common :as com]))

(defn initialized? [state]
  (not (nil? (get @state :ex-entities))))

(defn page [state]
  (if (initialized? state)
    [:div.row
     [:div.col-md-3 [lp/left-panel state]]
     [:div.col-md-8 [mp/middle-panel state]]]
    [:div "Loading"]))

(defn init-state [state]
  (GET "/admin/cc/entities"
       {:handler       #(swap! state assoc :ex-entities (:ex-entities %))
        :error-handler #(println "some error occured: " %)}))

(def varchar [(s/one (s/eq :varchar) "varchar") (s/one s/Num "varchar-length")])

(defn ^:export main []
  (init-state com/state)
  (reagent/render-component (fn [] [page com/state]) (h/get-elem "app")))
