(ns pfrt-js.core
  (:require [clojure.data :refer [diff]]
            [pfrt-js.templates :as tmpl]
            [pfrt-js.eventsource :as evs]
            [domina :refer [log html set-html! append! by-class
                            html-to-dom swap-content!]]
            [domina.css :refer [sel]]))

(def ^:static ^:private
  *remote-url* "/stream/stats")

(defn- render-layouts!
  "Render main layouts into DOM"
  []
  (set-html! (sel "body") (tmpl/base-layout))
  (set-html! (by-class "body-container") (tmpl/stats-layout)))

(defn- json->edn
  [data]
  (js->clj (.parse js/JSON data)))

(defn- sort-by-fn
  [attrname entry]
  (let [data (into {} (second entry))]
    (get data attrname)))

(def ^:private sort-by-downloaded (partial sort-by-fn "in"))
(def ^:private sort-by-upload (partial sort-by-fn "out"))

(defn- render-stats
  [_ _ oldval newval]
  (let [[fst scd _] (diff newval oldval)]
    (when (not (and (nil? fst) (nil? scd)))
      (log "first:" fst)
      (log "second:" scd)
      (let [el (html-to-dom "<tbody />")]
        (doseq [[host data] newval]
          (append! el (tmpl/host-entry host data)))
        (swap-content! (sel "tbody") el)))))


(defn ^:export main
  "Main entry point"
  []
  (let [sortfn     (atom sort-by-downloaded)
        stats-data (atom {})]
    (render-layouts!)

    ;; Add watch on data
    (add-watch stats-data :render render-stats)

    ;; Set initial layouts
    (let [es (evs/event-source *remote-url*)]
      (evs/listen! es "message" (fn [event]
                                  (let [data (json->edn (.-data event))]
                                    (swap! stats-data merge data)))))))
