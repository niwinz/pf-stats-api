(ns pfrt.settings
  (:require [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig cfg (io/resource "config.edn"))
