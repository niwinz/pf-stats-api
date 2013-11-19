(ns subprocess.httpserver
  (:require [compojure.core :as cc]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [org.httpkit.server :as httpkit]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [subprocess.packetfilter :as pf]
            [subprocess.util :as util])
  (:gen-class))

(def cors-headers {"Access-Control-Allow-Origin" "*"
                   "Access-Control-Expose-Headers" "x-requested-with,content-type"})

(defn render
  [body & {:keys [headers status] :or {headers {"content-type" "text/html"} status 200}}]
  {:body body
   :headers (into cors-headers headers)
   :status status})

(defn home-view
  [request]
  (render (slurp (io/resource "index.html"))))

(defn stats
  [request]
  (let [headers {"content-type" "application/json"}
        body    (json/write-str @pf/hosts-data)]
    (render body :headers headers :status 200)))

(defn stats-sse
  [request]
  (let [request-id  (util/random-uuid)
        headers     {"content-type" "text/event-stream"
                     "cache-control" "no-cache"}
        headers     (into headers cors-headers)]

    (httpkit/with-channel request channel
      (httpkit/on-close channel (fn [status]
                                  (remove-watch pf/last-packet (keyword request-id))
                                  (println "channel closed, " status)))

      ;; Send initial message with headers
      (httpkit/send! channel {:headers headers :status 200 :body "event: packet"} false)

      ;; Attach watcher for streaming events
      (add-watch pf/last-packet (keyword request-id)
                 (fn [_ _ _ packet]
                   (let [data (str "data:" (json/write-str packet) "\n\n")]
                     (httpkit/send! channel data false)))))))

(cc/defroutes app
  (cc/GET "/" req
    (home-view req))

  (cc/GET "/stats" req
    (stats req))

  (cc/GET "/stats-sse" req
    (stats-sse req))

  (route/resources "/static")
  (route/not-found "<h1>Page not found</h1>"))

(defn start-httpserver
  []
  (let [main-handler (handler/api app)]
    (println "Listening: http://localhost:9090/")
    (httpkit/run-server main-handler {:port 9090})))
