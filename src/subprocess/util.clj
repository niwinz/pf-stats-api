(ns subprocess.util
  (:import java.util.UUID)
  (:require [clojure.data.json :as json])
  (:gen-class))

(defn random-uuid []
  (let [uuid (.toString (UUID/randomUUID))]
    (.replaceAll uuid "-" "")))

(defn sleep [^Integer msec]
  (Thread/sleep msec))

(defn system-property
  [^String keyname & [default]]
  (System/getProperty keyname default))

(defn humanize-bytes
  [^Integer bytesnumber]
  (humanize.Humanize/binaryPrefix bytesnumber))

(defn json-dumps
  [data]
  (json/write-str data))

(defn json-loads
  [data]
  (json/read-str data :key-fn keyword))
