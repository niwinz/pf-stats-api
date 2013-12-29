(ns pfrt.util
  (:import java.util.UUID java.net.InetAddress))

(defn random-uuid []
  (let [uuid (.toString (UUID/randomUUID))]
    (.replaceAll uuid "-" "")))

(defn system-property
  [^String keyname & [default]]
  (System/getProperty keyname default))

(defn ip->hostname
  [^String ip]
  (let [addr (InetAddress/getByName (name ip))]
    (.getHostName addr)))
