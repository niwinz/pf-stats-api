(ns pfrt.core.app
  (:require [pfrt.core.lifecycle :as lifecycle])
  (:require [swiss.arrows :refer [-<>]]))

(defprotocol Application
  (init [_] "Initialize method")
  (start [_] "Start service")
  (stop [_] "Stop service"))

(defrecord App [components]
  Application
  (init [this]
    (-<> this (reduce #(lifecycle/init %2 %1) <> components)))
  (start [this]
    (-<> this (reduce #(lifecycle/start %2 %1) <> components)))
  (stop [this]
    (-<> this (reduce #(lifecycle/stop %2 %1) <> components))))
