(ns pfrt.util.monad
  (:require [clojure.algo.monads :refer [domonad maybe-m]]))

(defmacro maybe
  "Simple helper for maybe monad."
  [bindings & body]
  `(domonad maybe-m
     ~bindings
     (do ~@body)))
