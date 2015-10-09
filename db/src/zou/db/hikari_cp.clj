(ns zou.db.hikari-cp
  (:require [hikari-cp.core :as pool]
            [zou.component :as c]))

(defrecord HikariCP []
  c/Lifecycle
  (start [{:keys [datasource] :as this}]
    (if datasource
      this
      (assoc this :datasource (pool/make-datasource (into {} this)))))
  (stop [{:keys [datasource] :as this}]
    (when (instance? java.io.Closeable datasource)
      (.close datasource))
    (assoc this :datasource nil)))
