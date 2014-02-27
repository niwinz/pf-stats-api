(ns pfrt.main
  (:require [pfrt.pf :refer [packet-filter]]
            [pfrt.web :refer [web-server]]
            [pfrt.core.app :as app]
            [pfrt.settings :as settings])
  (:use clojure.tools.namespace.repl)
  (:gen-class))

;; Global var, only used for store
;; a global app instance when this is
;; used from REPL.
(def main-app nil)

;; System private constructor with
;; clear and explicit dependency injection
;; on each app components.
(defn- make-app
  []
  (let [config    (settings/cfg)
        pf        (packet-filter config)
        webserver (web-server config pf)]
    (-> (app/->App [pf webserver])
        (app/init))))

;; Start function that should
;; only be used from REPL.
(defn start
  []
  (alter-var-root #'main-app (constantly (make-app)))
  (app/start main-app))

;; Stop function that should
;; only be used from REPL.
(defn stop []
  (app/stop main-app))

;; Helper function that executes
;; stop and start.
(defn restart []
  (stop)
  (refresh :after 'pfrt.main/start))

;; Main entry point
(defn -main
  [& args]
  (let [app-instance (make-app)]
    (app/start app-instance)
    (println "Application started.")))
