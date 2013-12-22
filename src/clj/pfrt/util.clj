(ns pfrt.util
  (:import java.util.UUID
           java.net.InetAddress)
  (:require [clojure.data.json :as json]
            [clojure.algo.monads :refer [domonad maybe-m]])
  (:gen-class))

(defn random-uuid []
  (let [uuid (.toString (UUID/randomUUID))]
    (.replaceAll uuid "-" "")))

(defn sleep [^Integer msec]
  (Thread/sleep msec))

(defn system-property
  [^String keyname & [default]]
  (System/getProperty keyname default))

(defn json-dumps
  [data]
  (json/write-str data))

(defn json-loads
  [data]
  (json/read-str data :key-fn keyword))

(defmacro maybe
  "Simple helper for maybe monad."
  [bindings & body]
  `(domonad maybe-m
     ~bindings
     (do ~@body)))

(defn ip->hostname
  [^String ip]
  (let [addr (InetAddress/getByName (name ip))]
    (.getHostName addr)))
