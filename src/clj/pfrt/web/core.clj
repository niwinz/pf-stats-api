(ns pfrt.web.core
  (:require [pfrt.util :as util]))

(def cors-headers {"Access-Control-Allow-Origin" "*"
                   "Access-Control-Expose-Headers" "x-requested-with,content-type"})

;; (defmacro with-cors
;;   [& body]
;;   `(let [r# (do ~@body)]
;;      (if (:headers r#)
;;        (assoc r# :headers (merge (:headers r)
;;                                  (cors-headers)))
;;        r#)))

(defn- render
  [& {:keys [body headers status]}]
  {:body body
   :headers (into cors-headers headers)
   :status status})

(defn render-html
  ([body] (render-html body 200))
  ([body status]
   (render :body body
           :status status
           :headers {"content-type" "text/html"})))

(defn render-json
  ([data] (render-json data 200))
  ([data status]
   (render :body (util/json-dumps data)
           :status status
           :headers {"content-type" "application/json"})))
