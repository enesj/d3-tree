(ns foo.example.components.handler
  (:require [compojure.core :refer [defroutes]]
            [noir.response :refer [redirect]]
            [noir.util.middleware :refer [app-handler]]
            [ring.middleware.defaults :refer [site-defaults]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            ;[ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :refer [wrap-resource]]
            [compojure.route :as route]
            [com.stuartsierra.component :as comp]
            [foo.example.routes.home :refer [home-routes]]
            [foo.example.routes.jus :refer [jus-routes]]
            [foo.example.routes.cc :refer [cc-routes]]
            [foo.example.routes.user :refer [user-routes registration-routes]]
            [foo.example.middleware :refer [load-middleware]]))

(defroutes base-routes
           (route/files "/")
           (route/not-found "Not Found"))

;; timeout sessions after 30 minutes
(def session-defaults
  {:timeout          (* 60 30)
   :timeout-response (redirect "/")})

(defn- mk-defaults
  "set to true to enable XSS protection"
  [xss-protection?]
  (-> site-defaults
      (update-in [:session] merge session-defaults)
      (assoc-in [:security :anti-forgery] xss-protection?)))

(defn get-handler [config locale]
  (-> (app-handler
        (into [] (concat (when (:registration-allowed? config) [(registration-routes config)])
                         ;; add your application routes here
                         [(cc-routes config) jus-routes home-routes (user-routes config) base-routes]))
        ;; add custom middleware here
        :middleware (load-middleware config (:tconfig locale))
        :ring-defaults (mk-defaults false)
        ;; add access rules here
        :access-rules []
        ;; serialize/deserialize the following data formats
        ;; available formats:
        ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
        :formats [:json-kw :edn :transit-json])
      ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-resource "public")
      ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(defrecord Handler [config locale]
  comp/Lifecycle
  (start [comp]
    (assoc comp :handler (get-handler (:config config) locale)))
  (stop [comp]
    (assoc comp :handler nil)))

(defn new-handler []
  (map->Handler {}))
