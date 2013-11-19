(ns subprocess.util
  (:import java.util.UUID)
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
