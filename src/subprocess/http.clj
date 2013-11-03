(ns subprocess.http
  (:import java.util.UUID)
  (:require [compojure.core :as cc]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [org.httpkit.server :as httpkit]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [subprocess.core :as main]
            [hiccup.page :as hiccup-page]
            [hiccup.core :as hiccup])
  (:gen-class))


(defn home-view
  [request]
  (-> (io/resource "index.html") (slurp)))

(defn stats
  [request]
  (json/write-str @main/hosts-data))

(defn stats-sse
  [request]
  (let [request-id (doto (.toString (UUID/randomUUID))
                     (.replaceAll "-" ""))]
    (httpkit/with-channel request channel
      (httpkit/on-close channel (fn [status]
                                  (remove-watch main/last-packet (keyword request-id))
                                  (println "channel closed, " status)))

      ;; Send initial message with headers
      (httpkit/send! channel {:headers {"Content-Type" "text/event-stream"
                                        "Cache-Control" "no-cache"}
                              :status 200
                              :body "event: packet"} false)

      ;; Attach watcher for streaming events
      (add-watch main/last-packet (keyword request-id)
                 (fn [_ _ _ packet]
                   (let [data (str "data:" (json/write-str packet) "\n\n")]
                     (httpkit/send! channel data false)))))))

(cc/defroutes app
  (cc/GET "/" req
          (println req)
          (-> req home-view))
  (cc/GET "/stats" req (-> req stats))
  (cc/GET "/stats-sse" req (-> req stats-sse))
  (route/resources "/static")
  (route/not-found "<h1>Page not found</h1>"))

(def handler (handler/site app))

(defn -main
  [& args]
  ;; Start main process threads
  (main/start-speedcalculator)
  (main/start-collector)

  ;; Start http server
  (println "Listening: http://localhost:9090/")
  (httpkit/run-server handler {:port 9090}))
