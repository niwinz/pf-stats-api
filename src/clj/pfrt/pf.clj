(ns pfrt.pf
  (:require [clojure.java.io :as io]
            [pfrt.core.lifecycle :refer [ILifecycle]]
            [pfrt.util.monad :refer [maybe]]
            [pfrt.util :refer [ip->hostname]]
            [pfrt.executor :as executor])
  (:gen-class))

;; Global defintion of packet pattern
(def ^:private ^:static
  packet-pattern (re-pattern (str "^([^\\s]+)\\s"
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
      (assoc value host {:in 0
                         :out 0
                         :speed-in 0
                         :speed-out 0
                         :name (ip->hostname host)}))))

(defn s-packet-reader
  "Runnable that read packets from executed command and parses it."
  [config pfdata pflastpk]
  (let [command     (-> config :cmd)
        proc        (run-command command)
        reader      (io/reader (.getInputStream proc))
        read-packet (fn []
                      (maybe [raw-d (.readLine reader)
                              raw-p (re-find packet-pattern raw-d)
                              p     (parse-packet raw-p)]
                        (dosync
                          (send pflastpk (fn [_ p] p) p)
                          (alter pfdata update-data p))))]
    (executor/run-interval read-packet (-> config :sleep))))

(defn s-speed-calculator
  "Runnable that calculate speed over time of captured packets."
  [config pfdata pfspeed]
  (let [calcfn (fn []
                 (doseq [[host data] (seq (deref pfdata))]
                   (dosync
                     (let [diff-in  (- (:in data) (or (-> @pfspeed host :in) 0))
                           diff-out (- (:out data) (or (-> @pfspeed host :out) 0))]
                       (alter pfdata assoc-in [host :speed-in] (/ diff-in 1))
                       (alter pfdata assoc-in [host :speed-out] (/ diff-out 1))
                       (alter pfspeed assoc-in [host :in] (:in data))
                       (alter pfspeed assoc-in [host :out] (:out data))))))]
    (executor/run-interval calcfn 1000)))

(defrecord PacketFilter
  [reader-thread speedc-thread data speed lastpk]

  ILifecycle

  (init [this system]
    (assoc system :pf this))

  (start [_ system]
    (.start reader-thread)
    (.start speedc-thread)
    system)

  (stop [_ system]
    (.interrupt reader-thread)
    (.interrupt speedc-thread)
    system))

(defn packet-filter
  [config]
  (let [pfdata        (ref {})
        pfspeed       (ref {})
        pflastpk      (agent {})
        reader-thread (executor/thread (s-packet-reader config pfdata pflastpk))
        speedc-thread (executor/thread (s-speed-calculator config pfdata pfspeed))]
    (->PacketFilter reader-thread speedc-thread
                    pfdata pfspeed pflastpk)))
