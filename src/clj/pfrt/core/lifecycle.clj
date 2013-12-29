(ns pfrt.core.lifecycle)

(defprotocol ILifecycle
  (init [_ system] "Initialize method")
  (start [_ system] "Start service")
  (stop [_ system] "Stop service"))
