(ns pfrt.core)

(defprotocol Lifecycle
  (init [_ system] "Initialize method")
  (start [_ system] "Start service")
  (stop [_ system] "Stop service"))

