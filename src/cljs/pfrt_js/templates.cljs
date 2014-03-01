(ns pfrt-js.templates
  (:require [crate.core :refer [html]]
            [domina :refer [log]]
            [clojure.string :as str]))

(defn- hostname->domid
  "Convert host name to valid
  dom identifier."
  [host]
  (str "host" (str/join (str/split host #"\."))))

(defn host-entry
  [host data]
  (let [downloaded      (get data "in")
        uploaded        (get data "out")
        download-speed  (get data "speed-in")
        upload-speed    (get data "speed-out")]
    (html [:tr {:id (hostname->domid host)}
           [:td (:name data host)]
           [:td (js/filesize downloaded)]
           [:td (js/filesize uploaded)]
           [:td (str (js/filesize download-speed) "/s")]
           [:td (str (js/filesize upload-speed) "/s")]])))

(defn stats-layout
  []
  (html
   [:div {:class "container table-container"}
    [:table {:id "stats-container" :class "stats-container"}
     [:thead
      [:tr
       [:td "Hostname"]
       [:td "Download"]
       [:td "Upload"]
       [:td "Download Speed"]
       [:td "Upload Speed"]]]
     [:tbody]]]))

(defn base-layout
  "Main layout for all pages."
  []
  (html
   [:div {:class "wrapper"}
    [:div {:class "container header-container"}
     [:h1 "Pr0n Stats"]]
    [:div {:class "container body-container"}]]))
