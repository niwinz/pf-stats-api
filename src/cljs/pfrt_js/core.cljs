(ns pfrt-js.core
  (:use [purnam.cljs :only [aget-in aset-in]]
        [jayq.util :only [log]]
        [jayq.core :only [$ html replace-with append]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [clojure.data :refer [diff]]
            [clojure.string :as str]
            [purnam.types :as types])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [purnam.js :only [obj arr ! def.n def*n def* do*n]]
               [purnam.angular :only [def.module def.filter
                                      def.factory def.controller]]))

(def *remote-url* "/stream/stats")
(def *items* (atom {}))

(defn- host->id
  "Convert host name to valid
  dom identifier."
  [host]
  (str "host" (str/join (str/split host #"\."))))

(defn- render-host-element
  "Render one element."
  [host data]
  (let [downloaded      (:in data)
        uploaded        (:out data)
        download-speed  (:speed-in data)
        upload-speed    (:speed-out data)]
    (node [:tr {:id (host->id host)}
           [:td host]
           [:td (js/filesize downloaded)]
           [:td (js/filesize uploaded)]
           [:td (str (js/filesize download-speed) "/s")]
           [:td (str (js/filesize upload-speed) "/s")]])))

(defn- render
  "Render all matrix."
  [stats]
  (let [temporal-node ($ "<tbody />")
        sortfn        (fn [it]
                        (let [data (into {} (second it))]
                          (get data "in")))
        stats         (reverse
                        (sort-by sortfn (into [] stats)))]
    (doseq [[host data] stats]
      (append temporal-node (render-host-element host data)))
    temporal-node))

(defn- main
  []
  (let [event-source  (js/EventSource. *remote-url*)
        on-receive    (fn [e]
                        (let [dom-tbody ($ ".stats-container tbody")
                              data      (.parse js/JSON (.-data e))]
                          (replace-with dom-tbody (render data))))
        on-error      (fn [e]
                        (log "error" e))]
    (.addEventListener event-source "message" on-receive)))

(main)
