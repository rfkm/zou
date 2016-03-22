(ns zou.db.hikari-cp
  (:require [hikari-cp.core :as pool]
            [zou.component :as c]))

(defrecord HikariCP [datasource]
  c/Lifecycle
  (start [this]
    (if datasource
      this
      (map->HikariCP {:datasource (pool/make-datasource (into {} this))})))
  (stop [this]
    (when (instance? java.io.Closeable datasource)
      (.close datasource))
    (assoc this :datasource nil)))
