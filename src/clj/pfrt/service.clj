(ns pfrt.service
  (:require [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig cfg (io/resource "config.edn"))

(defn thread-sleep
  "Helper method for make sleep a current thread."
  [msecs]
  (Thread/sleep msecs))

(defn thread-interrumped?
  "Checks if a current thread has a interrumped
  flag activated."
  []
  (.isInterrupted (Thread/currentThread)))

(defn thread-name
  "Get name of current thread."
  []
  (.getName (Thread/currentThread)))

(defn start-thread
  "Given a function and arguments as vector, starts a new
  thread and returns it."
  [f args]
  (let [runnable  (fn [] (apply f args))
        t         (Thread. runnable)]
    (.start t)
    t))

(defn start-service
  "High level abstraction over ``start-thread``.
  Excecute a function in ont thread, and joins it
  depenging on optional parameters."
  [f & {:keys [join args] :or {join false args []} :as opts}]
  (let [t (start-thread f args)]
    (when join
      (.join t))
    t))

(defn run-interval
  "Execute a function repeatedly every one interval.

  This function use a safe way to execute a loop
  for execute in one thread, because it checks
  thread interruption on each loop iteration."
  ([f] (run-interval f 0))
  ([f msecs]
   (while (complement thread-interrumped?)
     (do
       (f)
       (when (> msecs 0)
         (thread-sleep msecs))))))
