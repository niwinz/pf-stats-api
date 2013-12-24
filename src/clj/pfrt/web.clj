(ns pfrt.web
  (:require [compojure.handler :as handler]
            [org.httpkit.server :as hkit]
            [pfrt.web.routes :refer [app]]
            [pfrt.service :as service]))

(defn s-web
  "Main runnable entry for start a standalone
  web server."
  []
  (let [ctrl (handler/api app)
        cfg  (service/cfg)
        port (get cfg :port 9090)]
    (println (format "Listening: http://localhost:%s/" port))
    (hkit/run-server ctrl {:port port})))
