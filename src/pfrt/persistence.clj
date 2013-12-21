(ns pfrt.persistence
  (:require [jdbc]
            [pfrt.util :as util])
  (:gen-class))

(def dbspec {:subprotocol "h2" :subname "file:p0rnstats.h2.db"})

(defn bootstrap
  []
  (let [sql (str "CREATE TABLE IF NOT EXISTS stats ("
                 "  data text, "
                 "  created_at timestamp default now());")]
    (jdbc/with-connection dbspec conn
      (jdbc/with-transaction conn
        (jdbc/execute! sql)))))


(defn persist
  [dataref]
  (let [data  (deref dataref)
        sql   "INSERT INTO stats (data) VALUES (?);"]
    (jdbc/with-connection dbspec conn
      (jdbc/with-transaction conn
        (jdbc/execute-prepared! sql [(util/json-dumps data)])))))
