(ns subprocess.ducksboard
  (:import java.net.InetAddress)
  (:require [subprocess.core :as core]
            [subprocess.util :as util]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [org.httpkit.client :as http])
  (:gen-class))

(defn ip->hostname
  [^String ip]
  (let [addr (InetAddress/getByName (name ip))]
    (.getHostName addr)))

(defn data->ducksboard-format
  [data]
  (let [units   (* 1024 1024)
        entries (for [[ip ipdata] (seq data)]
                  {:name (ip->hostname ip)
                   :values [(quot (:in ipdata) units)]})]
                            ;; (quot (:out ipdata) units)]})]})]]
    {:value {:board entries}}))

(defn ducksboard-updateloop
  []
  (let [token     (System/getProperty "subprocess.token" "")
        options   {:basic-auth [token "unused"]}]
    (loop []
      (let [data (deref core/hosts-data)
            data (data->ducksboard-format data)]
        (http/post "https://push.ducksboard.com/v/266990"
                   (assoc options :body (json/write-str data)))
        (util/sleep 15000))
      (recur))))

(defn start-ducksboard
  []
  (let [t (Thread. ducksboard-updateloop)]
    (.start t)
    t))
