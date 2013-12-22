(ns pfrt.web.routes
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources not-found]]
            [pfrt.web.views :as views])
  (:gen-class))

(defroutes app
  (GET "/" req
    (views/home-view req))

  (GET "/stats" req
    (views/stats req))

  (GET "/stream/stats" req
    (views/stats-stream req))

  (resources "/static")
  (not-found "<h1>Page not found</h1>"))
