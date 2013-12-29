(ns pfrt.web.views
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as hkit]
            [pfrt.executor :as executor]
            [pfrt.util :refer [random-uuid]]
            [pfrt.util.json :refer [json-dumps]]
            [pfrt.web.core :refer [render-json render-html cors-headers]])
  (:gen-class))

(defn home-view
  [request]
  (render-html (slurp (io/resource "index.html"))))

(defn stats
  [request]
  (let [pfctx (-> request :ctx :pf)]
    (render-json @(:pfdata pfctx))))

(defn stats-stream
  [request]
  (let [request-id  (random-uuid)
        headers     (merge cors-headers
                           {"content-type" "text/event-stream"
                            "cache-control" "no-cache"})
        initialmsg  {:headers headers
                     :body "event: message\n"
                     :status 200}
        pfctx       (-> request :ctx :pf)]
    (hkit/with-channel request channel
      (hkit/send! channel initialmsg false)
      (let [worker  (fn []
                      (let [loopfn (fn []
                                    (let [data (str "data: " (json-dumps @(:data pfctx)) "\n\n")]
                                      (hkit/send! channel data false)))]
                        (executor/run-interval loopfn 1000)))
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
