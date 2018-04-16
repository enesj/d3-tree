(ns reagent-tree.dev
  (:require [schema.core :as s]
            [foo.example.core :as core]))

(s/set-fn-validation! true)

(enable-console-print!)

(defn -main [] (core/main))
