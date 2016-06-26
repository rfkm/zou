(ns zou.db.hikari-cp
  (:require [hikari-cp.core :as pool]
            [zou.component :as c]
            [zou.db.tx :as tx]
            [zou.db.tx.jdbc :as tx-jdbc]
            [zou.util :as u]))

(defrecord HikariCP [datasource]
  c/Lifecycle
  (start [this]
    (if datasource
      this
      (map->HikariCP {:datasource (pool/make-datasource (into {} this))
                      :spec (into {} this) ; keep config map for reference
                      })))
  (stop [this]
    (when (instance? java.io.Closeable datasource)
      (.close datasource))
    (assoc this :datasource nil))

  tx/Tx
  (-id [this]
    this)
  (-atomic [this ctx f]
    (tx-jdbc/ctx-transaction (u/weak-assoc ctx :db this) f)))
