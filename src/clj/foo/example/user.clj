(ns foo.example.user
  (:require [reloaded.repl :refer [go reset stop]]
          [foo.example.components.components :refer [dev-system]]
          [schema.core :as s]))

(defn start-dev-system []
  (s/set-fn-validation! true)
  (go))

(reloaded.repl/set-init! dev-system)

()