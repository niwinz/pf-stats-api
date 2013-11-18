(ns subprocess.util
  (:import java.util.UUID)
  (:gen-class))

(defn random-uuid []
  (let [uuid (.toString (UUID/randomUUID))]
    (.replaceAll uuid "-" "")))

(defn sleep [^Integer msec]
  (Thread/sleep msec))
