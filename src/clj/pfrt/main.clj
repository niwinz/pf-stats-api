(ns pfrt.main
  (:require [pfrt.packetfilter :as pf]
            [pfrt.web :as web]
            [pfrt.util :as util]
            [pfrt.service :as service])
  (:gen-class))

(defn -main
  [& args]

  ;; Packet Filter Service Threads
  (service/start-service pf/s-speed-calculator)
  (service/start-service pf/s-packet-reader)
  ;; Start main process threads
  ;; (dk/start-ducksboard)
  ;;
  (service/start-service web/s-web :join true))
