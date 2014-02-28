(ns pfrt-js.eventsource)

(defn event-source
  [uri]
  (js/EventSource. uri))

(defn listen!
  [evs eventname callback]
  (.addEventListener evs eventname callback)
  evs)
