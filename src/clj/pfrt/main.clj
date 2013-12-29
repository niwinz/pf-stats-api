(ns pfrt.main
  (:require [pfrt.pf :refer [packet-filter]]
            [pfrt.web :refer [web-server]]
            [pfrt.core :refer [->System]]
            [pfrt.settings :as settings])
  (:gen-class))

;; Global var, only used for store
;; a global system instance when this is
;; used from REPL.
(def system nil)

;; System private constructor with
;; clear and explicit dependency injection
;; on each system components.
(defn- make-system []
  (let [config    (settings/cfg)
        pf        (packet-filter config)
        webserver (web-server config pf)]
    (-> (->System [pf web-server])
        (assoc :config config)
        (init))))

;; Start function that should
;; only be used from REPL.
(defn start []
  (alter-var-root #'system
    (constantly (make-system)))
  (start system))

;; Stop function that should
;; only be used from REPL.
(def stop []
  (stop system))

;; Main entry point
(defn -main
  [& args]
  (let [system (make-system)]
    (start system)
    (println "System started")))
