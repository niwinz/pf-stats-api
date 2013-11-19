(ns subprocess.main
  (:require [subprocess.packetfilter :as pf]
            [subprocess.ducksboard :as dk]
            [subprocess.httpserver :as http]
            [subprocess.util :as util])
  (:gen-class))

(defn -main
  [& args]
  ;; Start main process threads
  (pf/start-packetfilter)
  (dk/start-ducksboard)
  (http/start-httpserver))
