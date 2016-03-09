(ns zou.framework.bootstrap.impl
  (:require [schema.core :as s]
            [zou.component :as c]
            [zou.framework.entrypoint.proto :as ep]))

(s/defrecord DefaultBootstrapSystem [entrypoint :- (s/protocol ep/EntryPoint)]
  {s/Keyword s/Any}
  c/Lifecycle
  (start [this]
    (s/validate (class this) this)
    (c/start-system this))
  (stop [this]
    (c/stop-system this))
  ep/EntryPoint
  (run [this args]
    (ep/run entrypoint args)))

(defn new-bootstrap-system
  "Create an instance of DefaultBootstrapSystem with the given config
  map."
  [conf]
  (map->DefaultBootstrapSystem (c/build-system-map conf)))
