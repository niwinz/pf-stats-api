(ns subprocess.ducksboard
  (:import java.net.InetAddress)
  (:require [subprocess.packetfilter :as pf]
            [subprocess.util :as util]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [org.httpkit.client :as http])
  (:gen-class))

;; TODO: logging instead of prints?

(def ^:dynamic *token*)
(def ^:dynamic *sleep-time*)

(defn ip->hostname
  [^String ip]
  (let [addr (InetAddress/getByName (name ip))]
    (.getHostName addr)))

(defn data->ducksboard
  [data dir]
  {:pre [(or (= dir :in) (= dir :out))]}
  (let [units   (* 1024 1024)
        entries (for [[ip ipdata] (seq data)]
                  {:name (ip->hostname ip)
                   :values [(dir ipdata)
                            (util/humanize-bytes (dir ipdata))]})
        sort-fn (fn [x] (-> x (:values) (get 0)))]
    {:value {:board (take 10 (reverse (sort-by sort-fn entries)))}}))

(defn do-ducksboard-update
  []
  (binding [*token*       (util/system-property "ducksboard.token")
            *sleep-time*  (util/system-property "ducksboard.interval" "60000")]
    (if (nil? *token*)
      (println "Ducksboard token is not set, disabling service.")
      (let [options {:basic-auth [*token* "unused"]}]
        (println (format "Ducksboard service enabled with interval of %s msecs." *sleep-time*))
        (loop []
          (let [data      (deref pf/hosts-data)
                data-in   (data->ducksboard data :in)
                data-out  (data->ducksboard data :out)]

            ;; (http/post "https://push.ducksboard.com/v/266990"
            ;;            (assoc options :body (json/write-str data-in)))
            ;; (http/post "https://push.ducksboard.com/v/268310"
            ;;            (assoc options :body (json/write-str data-out)))
            (http/post "https://push.ducksboard.com/v/268401"
                       (assoc options :body (json/write-str data-in)))
            (http/post "https://push.ducksboard.com/v/268402"
                       (assoc options :body (json/write-str data-out)))
            (util/sleep (Integer/parseInt *sleep-time*)))
          (recur))))))

(defn start-ducksboard
  []
  (let [t (Thread. do-ducksboard-update)]
    (.start t)
    t))