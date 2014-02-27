(ns pfrt-js.core
  (:use [purnam.cljs :only [aget-in aset-in]]           ;; required import (dependency)
        [jayq.util :only [log]]                         ;; console.log wrapper
        [jayq.core :only [$ html replace-with append]]) ;; dom manipulation
  (:require [clojure.data :refer [diff]]
            [clojure.string :as str]
            [purnam.types :as types]                    ;; extend js types with ISequable
            [crate.core :as crate]))                    ;; templating

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
    (crate/html [:tr {:id (host->id host)}
                 [:td (:name data)]
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
