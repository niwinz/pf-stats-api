(ns pfrt.pf
  (:require [clojure.java.io :as io]
            [pfrt.util :as util]
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
                         :name (util/ip->hostname host)}))))

(defn s-packet-reader
  "Runnable that read packets from executed command and parses it."
  [pfdata pflastpk]
  (let [command     (-> (executor/cfg) :cmd)
        proc        (run-command command)
        reader      (io/reader (.getInputStream proc))
        read-packet (fn []
                      (util/maybe [raw-d (.readLine reader)
                                   raw-p (re-find packet-pattern raw-d)
                                   p     (parse-packet raw-p)]
                        (dosync
                          (send pflastpk (fn [_ p] p) p)
                          (alter pfdata update-data p))))]
    (executor/run-interval read-packet (-> (executor/cfg) :sleep))))

(defn s-speed-calculator
  "Runnable that calculate speed over time of captured packets."
  [pfdata pfspeed]
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

(defrecord PacketFilter [data speed lastpk]
  Lifecycle

  (init [_ system] system
    (let [t-packet-reader     (thread (s-packet-reader data lastpk))
          t-speed-calculator  (thread (s-speed-calculator data speed))]
      (.setDaemon t-packet-reader false)
      (.setDaemon t-speed-calculator false)
      (-> system
          (assoc ::pf-reader-thread t-packet-reader)
          (assoc ::pf-speedc-thread t-speed-calculator))))

  (start [_ system]
    (.start (::pf-reader-thread system))
    (.start (::pf-speedc-thread system))
    (-> system
        (assoc ::pf-data data)
        (assoc ::pf-data-last lastpk)))

  (stop [_ system]
    (.interrupt (::pf-reader-thread system))
    (.interrupt (::pf-speedc-thread system))
    system))

(defn packet-filter []
  (->PacketFilter (ref {}) (ref {}) (agent {})))
