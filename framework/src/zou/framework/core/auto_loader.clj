(ns zou.framework.core.auto-loader
  (:require [zou.component :as c]
            [zou.util.namespace :as ns]))

(defrecord AutoLoader [exclude-classpath prefixes]
  c/Lifecycle
  (start [this]
    (when (seq prefixes)
      (apply ns/require-all exclude-classpath prefixes))
    this)
  (stop [this]
    this))
