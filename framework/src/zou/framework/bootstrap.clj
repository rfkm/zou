(ns zou.framework.bootstrap
  (:require [schema.core :as s]
            [zou.component :as c]
            [zou.framework.entrypoint.proto :as ep]))

(def BootstrapConfig {s/Keyword s/Any})

(def BootstrapSystem (s/constrained (s/protocol c/Lifecycle)
                                    ep/entrypoint?))

(s/defn ^:always-validate
  make-bootstrap-system :- BootstrapSystem
  "Create an instance of bootstrap system using a constructor which is
  specified via `:zou/constructor` key of the given config map. If
  it's not specified, `com.stuartsierra.component/map->SystemMap` will
  be used."
  [conf :- BootstrapConfig]
  (c/build-system-map conf))

(s/defn ^:always-validate
  init-bootstrap-system :- BootstrapSystem
  "Start a life cycle of the given bootstrap system."
  [sys :- BootstrapSystem]
  (c/try-recovery (c/start sys)))

(s/defn deinit-bootstrap-system :- s/Any
  "Stop a life cycle of the given bootstrap system."
  [sys :- BootstrapSystem]
  (c/stop sys))

(s/defn run-bootstrap-system :- s/Any
  "Run main process of the given bootstrap system with
  `zou.framework.entrypoint.proto/EntryPoint#run`."
  [sys :- BootstrapSystem
   & args :- [s/Str]]
  (ep/run sys args))
