(ns pfrt-js.core
  (:use [purnam.cljs :only [aget-in aset-in]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [clojure.data :refer [diff]])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [purnam.js :only [obj arr ! def.n def*n def* do*n]]
               [purnam.angular :only [def.module def.filter
                                      def.factory def.controller]]))
