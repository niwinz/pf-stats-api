(ns pfrt.executor)

(defn thread-sleep
  "Helper method for make sleep a current thread."
  [msecs]
  (Thread/sleep msecs))

(defn thread-name
  "Get name of current thread."
  []
  (.getName (Thread/currentThread)))

(defn thread-interrumped?
  "Checks if a current thread has a interrumped
  flag activated."
  []
  (.isInterrupted (Thread/currentThread)))

(defmacro thread
  [& body]
  `(let [f# (fn [] ~@body)
         t# (Thread. f#)]
     (.setDaemon t# false)
     t#))

(defmacro daemon-thread
  [& body]
  `(let [t# (thread ~@body)]
     (.setDaemon t# true)
     t#))

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
