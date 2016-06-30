(ns foo.example.web.setup
  (:require [clj-webdriver.taxi :as w]
            [joplin.alias :as a]
            [joplin.repl :as r]
            [taoensso.tower :as tower]
            [com.stuartsierra.component :as component]
            [reloaded.repl :refer [go stop]]
            [foo.example.components.server :refer [new-web-server]]
            [foo.example.components.handler :refer [new-handler]]
            [foo.example.components.config :as c]
            [foo.example.components.db :refer [new-db]]
            [foo.example.components.components :refer [prod-system]]
            [foo.example.components.locale :as l]))

(def db-uri "jdbc:sqlite:./db/reagent-tree-integtest.sqlite")
(def jop-config (a/*load-config* "joplin.edn"))

; custom config for configuration
(def test-config
  {:hostname                "http://localhost/"
   :mail-from               "info@localhost.de"
   :mail-type               :test
   :activation-mail-subject "Please activate your account."
   :activation-mail-body    "Please click on this link to activate your account: {{activationlink}}
Best Regards,

Your Team"
   :activation-placeholder  "{{activationlink}}"
   :smtp-data               {}                                ; passed directly to postmap like {:host "postfix"}
   :jdbc-url                db-uri
   :env                     :dev
   :registration-allowed?   true
   :captcha-enabled?        false
   :captcha-public-key      "your public captcha key"
   :private-recaptcha-key   "your private captcha key"
   :recaptcha-domain        "yourdomain"
   :port                    3001})


(defn test-system []
  (component/system-map
    :locale (l/new-locale)
    :config (c/new-config test-config)
    :db (component/using (new-db) [:config])
    :handler (component/using (new-handler) [:config :locale])
    :web (component/using (new-web-server) [:handler :config])))

(def test-base-url (str "http://localhost:3001/"))

(defn start-browser [browser]
  (r/reset jop-config :sqlite-integtest-env :sqlite-integtest)
  (w/set-driver! {:browser browser}))

(defn stop-browser []
  (w/quit))

(defn start-server []
  (reloaded.repl/set-init! test-system)
  (go))

(defn stop-server []
  (stop))

(defn server-setup [f]
  (start-server)
  (f)
  (stop-server))

(defn browser-setup [f]
  (start-browser :htmlunit)
  (f)
  (stop-browser))

;; locale stuff

(def t (tower/make-t l/tconfig))
