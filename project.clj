(defproject d3-tree "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://www.bas.gov.ba/jus"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.521"]
                 [org.clojure/core.cache "0.7.1"]
                 [org.clojure/core.async "0.4.474"]
                 [ring "1.6.3"]
                 [lib-noir "0.9.9"]
                 [ring/ring-anti-forgery "1.2.0"]
                 [compojure "1.6.0"]
                 [rum "0.11.2" :exclusions [org.clojure/tools.reader cljsjs/react cljsjs/react-dom]]
                 [reagent "0.7.0" :exclusions [org.clojure/tools.reader cljsjs/react cljsjs/react-dom]]
                 [cljs-react-material-ui "0.2.45"]
                 [cljsjs/d3 "3.5.16-0"] ; ne diraj verziju
                 [environ "1.1.0"]
                 [leiningen "2.8.1"]
                 [http-kit "2.2.0"]
                 [selmer "1.11.7"]
                 [prone "1.5.1"]
                 [im.chit/cronj "1.4.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tempura "1.2.0"]
                 [com.taoensso/tower "3.0.2"]
                 [noir-exception "0.2.5"]
                 [buddy/buddy-auth "2.1.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 [binaryage/devtools "0.9.10"]
                 ;[binaryage/dirac "1.2.33"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.clojure/java.jdbc "0.7.5"]
                 [korma "0.4.3"]
                 [org.xerial/sqlite-jdbc "3.21.0.1"]
                 [com.draines/postal "2.0.2"]
                 [jarohen/nomad "0.7.3"]
                 [de.sveri/clojure-commons "0.2.2"]
                 ;[clojure-miniprofiler "0.5.0"]
                 [org.danielsz/system "0.4.1"]
                 [datascript "0.16.4"]
                 [cljs-ajax "0.7.3"]
                 [ring-transit "0.1.6"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [net.tanesha.recaptcha4j/recaptcha4j "0.0.8"]
                 [midje "1.9.1"]
                 [spec-provider "0.4.11"]
                 [org.clojure/test.check "0.9.0"]
                 ;[org.clojure/core.typed "0.5.0"]  ; Onemoguciti kada se koristi piggiback
                 [org.clojure/core.typed "0.3.11"]
                 [reloaded.repl "0.2.4"]
                 [prismatic/plumbing "0.5.5"]
                 [prismatic/schema "1.1.9"]
                 [com.rpl/specter "1.1.0"]
                 [joplin.jdbc "0.3.10"]
                 [joplin.core "0.3.10"]
                 [cljsjs/pdfmake "0.1.26-0"]
                 [alandipert/intension "1.1.1"]]
                 ;[de.sveri/closp-crud "0.3.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-midje "3.2.1"]
            [lein-typed "0.4.2"]]

  :min-lein-version "2.5.3"

  ; leaving this commented because of: https://github.com/cursiveclojure/cursive/issues/369
  ;:hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild
    {:builds {:dev {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                    :figwheel     {:on-jsload  "reagent-tree.dev/main"}
                    :compiler     {:main            "reagent-tree.dev"
                                   :asset-path      "/js/compiled/out"
                                   :output-to       "resources/public/js/compiled/app-new.js"
                                   :output-dir      "resources/public/js/compiled/out"
                                   :source-map      true
                                   :source-map-timestamp true
                                   :external-config {:devtools/config {:features-to-install :all}}
                                   :preloads       [devtools.preload]}}

              :adv {:source-paths ["src/cljs" "src/cljc"]
                    :compiler     {:output-to     "resources/public/js/compiled/app-new.js"
                                   ; leaving this commented because of: https://github.com/cursiveclojure/cursive/issues/369
                                   ;:jar           true
                                   :externs ["externs/d3_externs.js"]
                                   :optimizations :advanced
                                   :pretty-print  false}}}}
                                   ;:pseudo-names true}}}}
  :figwheel {:css-dirs   ["resources/public/css"]} ;; watch and update CSS

  :profiles {:dev     {:repl-options {:init-ns          foo.example.user
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]} ; Onemoguciti kada se koristi core.typed
                       :plugins      [[lein-ring "0.12.4" :exclusions [org.clojure/clojure]]
                                      [lein-figwheel "0.5.15" :exclusions [org.clojure/clojure]]]
                                      ;[test2junit "1.1.1"]]
                       :dependencies [[org.bouncycastle/bcprov-jdk15on "1.59"]
                                      [org.apache.httpcomponents/httpclient "4.5.5"]
                                      ;[clj-webdriver "0.7.2"]
                                      ;[org.seleniumhq.selenium/selenium-java "2.48.2"]
                                      [com.cemerick/piggieback "0.2.2"] ; Onemoguciti kada se koristi core.typed
                                      [org.clojure/tools.nrepl "0.2.10"] ; Onemoguciti kada se koristi core.typed
                                      [figwheel-sidecar "0.5.15"]
                                      [ring-mock "0.1.5"]
                                      [ring/ring-devel "1.6.3"]
                                      [pjstadig/humane-test-output "0.8.3"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]}
             :uberjar {:auto-clean  false                   ; not sure about this one
                       :omit-source true
                       :aot         :all}}

  :test-paths ["test/clj" "integtest/clj"]

  :test-selectors {:unit        (complement :integration)
                   :integration :integration
                   :cur         :cur                        ; one more selector for, give it freely to run only
                   ; the ones you need currently
                   :all         (constantly true)}

  :test2junit-output-dir "test-results"

  :main foo.example.core

  :uberjar-name "reagent-tree.jar"

  :aliases {"rel-jar"  ["do" "clean," "cljsbuild" "once" "adv," "uberjar"]
            "unit"     ["do" "test" ":unit"]
            "integ"    ["do" "test" ":integration"]

            ; migration utilities
            "migrate"  ["run" "-m" "joplin.alias/migrate" "joplin.edn" "sqlite-dev-env" "sqlite-dev"]
            "rollback" ["run" "-m" "joplin.alias/rollback" "joplin.edn" "sqlite-dev-env" "sqlite-dev"]
            "reset"    ["run" "-m" "joplin.alias/reset" "joplin.edn" "sqlite-dev-env" "sqlite-dev"]})
