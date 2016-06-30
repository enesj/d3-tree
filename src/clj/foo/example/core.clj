(ns foo.example.core
  (:require [taoensso.timbre :as timbre]
            [reloaded.repl :refer [go]]
            [foo.example.cljccore :as cljc]
            [foo.example.components.components :refer [prod-system]])
  (:gen-class))

(defn -main [& args]
  (reloaded.repl/set-init! prod-system)
  (go)
  (cljc/foo-cljc "hello from cljx")
  (timbre/info "server started."))
