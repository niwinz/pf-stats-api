(ns pfrt.web.views
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as hkit]
            [org.httpkit.timer :as htimer]
            [pfrt.packetfilter :as pf]
            [pfrt.util :as util]
            [pfrt.service :as service]
            [pfrt.web.core :refer [render-json render-html cors-headers]])
  (:gen-class))

(defn home-view
  [request]
  (render-html (slurp (io/resource "index.html"))))

;; FIXME
(defn stats
  [request]
  (render-json @pf/hosts-data))

(defn stats-stream
  [request]
  (println request)
  (let [request-id  (util/random-uuid)
        headers     (merge cors-headers
                           {"content-type" "text/event-stream"
                            "cache-control" "no-cache"})
        initialmsg  {:headers headers
                     :body "event: message\n"
                     :status 200}]
    (hkit/with-channel request channel
      (hkit/send! channel initialmsg false)
      (let [worker  (fn []
                      (println ".")
                      (let [loopfn (fn []
                                    (let [data (str "data: " (util/json-dumps @pf/hosts-data) "\n\n")]
                                      (hkit/send! channel data false)))]
                        (service/run-interval loopfn 1000)
                        (println "CLOSE")))
            thread  (Thread. worker)]
        (hkit/on-close channel (fn [& args]
                                 (.interrupt thread)))
        (.start thread)))))

;; (defn stats-stream
;;   "View that exposes stream of packets using
;;   Server-Sent Event protocol."
;;   [request]
;;   (let [request-id  (util/random-uuid)
;;         headers     (merge cors-headers
;;                            {"content-type" "text/event-stream"
;;                             "cache-control" "no-cache"})
;;         initialmsg  {:headers headers
;;                      :body "event: packeg\n"
;;                      :status 200}
;;         on-close    (fn [s]
;;                       (remove-watch pf/last-packet
;;                                     (keyword request-id)))]
;;     (hkit/with-channel request channel
;;       (hkit/on-close channel on-close)
;;       (hkit/send! channel initialmsg false)
;;       (add-watch pf/last-packet
;;                  (keyword request-id)
;;                  (fn [_ _ _ pk]
;;                    (let [message (str "data:" (util/json-dumps pk) "\n\n")]
;;                      (hkit/send! channel message false)))))))
