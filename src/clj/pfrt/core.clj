(ns pfrt.core
  (:require [swiss.arrows :refer [-<>]]))

(defprotocol IComponentLifecycle
  (init [_ system] "Initialize method")
  (start [_ system] "Start service")
  (stop [_ system] "Stop service"))

(defprotocol ISystemLifecycle
  (init [_] "Initialize method")
  (start [_] "Start service")
  (stop [_] "Stop service"))

(defrecord System [components]
  ISystemLifecycle
  (init [this]
    (-<> this (reduce #(init %1 %2) <> components)))
  (start [this]
    (-<> this (reduce #(start %1 %2) <> components)))
  (stop [this]
    (-<> this (reduce #(stop %1 %2) <> components))))
