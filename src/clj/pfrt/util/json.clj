(ns pfrt.util.json
  "Generic utils namespace that conains a set of
  distinct functions that are used on all aplications."
  (:require [clojure.data.json :as json]))

(defn json-dumps
  [data]
  (json/write-str data))

(defn json-loads
  [data]
  (json/read-str data :key-fn keyword))
