(ns subprocess.core
  (:require [clojure.java.io :as io])
  (:gen-class))


;; Global var containgin a command to exec
;; on the collector thread.
(def ^:dynamic *command*)
;; (def ^:dynamic *command* "tcpdump -n -e -ttt -i pflog0")

;; Global agent that stores all
;; collected and parsed data.
(def hosts-data (agent {}))

;; Global agent that stores
(def hosts-speed (agent {}))

;; Global atom that stores the last
;; packet entry
(def last-packet (atom {}))

;; Global defintion of packet pattern
(def ^:static packet-pattern (re-pattern (str "^([^\\s]+)\\s"
                                              "(rule\\s[^\\s]+)\\s"
                                              "(\\w+)\\s(\\w+)\\s\\w+\\s([\\w\\d]+)\\:\\s"
                                              "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.(\\d+)\\s"
                                              "[\\>\\<]\\s"
                                              "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.(\\d+)"
                                              ".*"
                                              "length\\s(\\d+)$")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Collector
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-command
  "Run shell command and return a process instance."
  [^String cmd]
  (let [runtime (Runtime/getRuntime)]
    (.exec runtime cmd)))

(defn make-entry
  "Transform received vector to map."
  [[_ _ dir iface srcaddr srcport dstaddr dstport size]]
  (let [main-host (if (= dir "in") srcaddr dstaddr)]
    (Thread/sleep 100)
    (when-not (or (= main-host "0.0.0.0") (.endsWith main-host ".255"))
      {:key main-host :data {:srcadrr srcaddr :dstaddr dstaddr :srcport srcport :dstport dstport
                             :size (Integer/parseInt size) :direction (if (= dir "in") "out" "in")}})))
(defn update-entry
  "Agent update method"
  [value entry]
  (let [data (:data entry)
        host (keyword (:key entry))
        dir  (keyword (:direction data))
        n    (:size data)]
    (if (contains? value host)
      (update-in value [host dir] (partial + n))
      (assoc value host {:in 0 :out 0 :speed-in 0 :speed-out 0}))))

(defn read-packet
  [reader]
  (let [line (.readLine reader)]
    (when line
      (re-find packet-pattern line))))

(defn collector
  "Runnable that read packets from executed command and parses it."
  []
  (binding [*command* (System/getProperty "subprocess.command" "cat pflog.txt")]
    (let [proc    (run-command *command*)
          reader  (io/reader (.getInputStream proc))]
      (loop []
        (let [result (read-packet reader)]
          (when (seq result)
            (let [entry (make-entry (drop 2 result))]
              (when entry
                (reset! last-packet entry)
                (send hosts-data update-entry entry))))
        (recur))))))

(defn start-collector []
  "Start collector thread and return it."
  (let [t (Thread. collector)]
    (.start t)
    t))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Speed calculator
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn speedcalculator
  "Runnable that calculate speed over time of captured packets."
  []
  (let [sleep-time  1]
    (loop []
      (let [snapshot @hosts-data]
        (doseq [[host data] (seq snapshot)]
          (if-not (contains? @hosts-speed host)
            ;; Set initial data
            (send hosts-speed assoc-in [host] {:in (:in data)
                                               :out (:out data)})
            (let [diff-in   (- (:in data) (-> @hosts-speed host :in))
                  diff-out  (- (:out data) (-> @hosts-speed host :out))]
              (send hosts-data assoc-in [host :speed-in] (/ diff-in sleep-time))
              (send hosts-data assoc-in [host :speed-out] (/ diff-out sleep-time))
              (send hosts-speed assoc-in [host :in] (:in data))
              (send hosts-speed assoc-in [host :out] (:out data))))))

      (Thread/sleep (* sleep-time 1000))
      (recur))))

(defn start-speedcalculator []
  "Start speed calculator in one thread and returns it."
  (let [t (Thread. speedcalculator)]
    (.start t)
    t))
