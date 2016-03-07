(ns zou.framework.core.logging
  (:require [zou.component :as c]
            [zou.logging :as log]
            [zou.util.repl :as ur]))

(defrecord LoggingConfigurator [bridge-out?]
  c/Lifecycle
  (start [this]
    (when bridge-out?
      (ur/bridge-out!))
    (log/start-logging! (dissoc (into {} this)
                                :bridge-out?))
    this)
  (stop [this]
    (ur/restore-out!)
    this))
