(ns pfrt.packetfilter
  (:require [clojure.java.io :as io]
            [pfrt.util :as util]
            [pfrt.service :as service])
  (:gen-class))

;; Global agent that stores all
;; collected and parsed data.
(def hosts-data (ref {}))

;; Global agent that stores
(def hosts-speed (ref {}))

;; Global atom that stores the last
;; packet entry
(def last-packet (agent {}))

;; Global defintion of packet pattern
(def ^:static packet-pattern (re-pattern (str "^([^\\s]+)\\s"
                                              "(rule\\s[^\\s]+)\\s"
                                              "(\\w+)\\s(\\w+)\\s\\w+\\s([\\w\\d]+)\\:\\s"
                                              "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.(\\d+)\\s"
                                              "[\\>\\<]\\s"
                                              "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.(\\d+)"
                                              ".*"
                                              "length\\s(\\d+)$")))

(defn- run-command
  "Run shell command and return a process instance."
  [^String cmd]
  (let [runtime (Runtime/getRuntime)]
    (.exec runtime cmd)))

(defn- parse-packet
  "Transform received vector to map."
  [[_ _ _ _ dir iface srcaddr srcport dstaddr dstport size]]
  (let [main-host (if (= dir "in") srcaddr dstaddr)]
    (when-not (or (= main-host "0.0.0.0") (.endsWith main-host ".255"))
      {:key main-host :data {:srcadrr srcaddr :dstaddr dstaddr :srcport srcport :dstport dstport
                             :size (Integer/parseInt size) :direction (if (= dir "in") "out" "in")}})))
(defn- update-data
  "Ref update method"
  [value entry]
  (let [data (:data entry)
        host (keyword (:key entry))
        dir  (keyword (:direction data))
        n    (:size data)]
    (if (contains? value host)
      (update-in value [host dir] (partial + n))
      (assoc value host {:in 0 :out 0 :speed-in 0 :speed-out 0}))))

(defn s-packet-reader
  "Runnable that read packets from executed command and parses it."
  []
  (let [command     (util/system-property "pfrt.command" "cat pflog.txt")
        proc        (run-command command)
        reader      (io/reader (.getInputStream proc))
        read-packet (fn []
                      (util/maybe [raw-d (.readLine reader)
                                   raw-p (re-find packet-pattern raw-d)
                                   p     (parse-packet raw-p)]
                        (dosync
                          (send last-packet (fn [_ p] p) p)
                          (alter hosts-data update-data p))))]
    (service/run-interval read-packet 100)))

(defn s-speed-calculator
  "Runnable that calculate speed over time of captured packets."
  []
  (let [calcfn (fn []
                 (doseq [[host data] (seq (deref hosts-data))]
                   (dosync
                     (let [diff-in  (- (:in data) (or (-> @hosts-speed host :in) 0))
                           diff-out (- (:out data) (or (-> @hosts-speed host :out) 0))]
                       (alter hosts-data assoc-in [host :speed-in] (/ diff-in 1))
                       (alter hosts-data assoc-in [host :speed-out] (/ diff-out 1))
                       (alter hosts-speed assoc-in [host :in] (:in data))
                       (alter hosts-speed assoc-in [host :out] (:out data))))))]
    (service/run-interval calcfn 1000)))
