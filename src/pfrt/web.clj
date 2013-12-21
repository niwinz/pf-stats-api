(ns pfrt.web
  (:require [compojure.handler :as handler]
            [org.httpkit.server :as hkit]
            [pfrt.web.routes :refer [app]]))

(defn s-web
  "Main runnable entry for start a standalone
  web server."
  []
  (let [ctrl (handler/api app)]
    (println "Listening: http://localhost:9090/")
    (hkit/run-server ctrl {:port 9090})))
