(ns pfrt.web
  (:require [compojure.handler :as handler]
            [org.httpkit.server :as hkit]
            [pfrt.web.routes :refer [app]]
            [pfrt.core.lifecycle :refer [ILifecycle]]))

(defrecord Web
  [webserver app]

  ILifecycle

  (init [this system]
    (assoc system :web this))

  (start [this system]
    (let [port    (get-in system [:config :port] 9090)
          stopfn  (hkit/run-server app {:port port})]
      (swap! webserver (fn [_] stopfn))
      (println (format "Listening: http://localhost:%s/" port))
      system))

  (stop [_ system]
    (let [stopfn (deref webserver)]
      (stopfn)
      system)))

(defn web-server
  [config pf]
  (let [wrap-context (fn [h]
                       (fn [req]
                         (h (assoc req :ctx {:pf pf}))))]
    (->Web (atom nil) (handler/api (wrap-context app)))))
