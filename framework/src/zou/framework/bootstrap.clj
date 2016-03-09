(ns zou.framework.bootstrap
  (:require [schema.core :as s]
            [zou.component :as c]
            [zou.framework.entrypoint.proto :as ep]
            [zou.util.namespace :as un]))

(def BootstrapConfig {:zou/constructor (s/pred (some-fn symbol?
                                                        var?
                                                        fn?))
                      s/Keyword s/Any})

(def BootstrapSystem (s/constrained (s/protocol c/Lifecycle)
                                    ep/entrypoint?))

(defn- resolve-ctor [sym]
  (try
    (un/resolve-var sym)
    (catch Throwable e
      (throw (IllegalArgumentException.
              (str "Unable to resolve var: " sym)
              e)))))

(s/defn ^:always-validate
  make-bootstrap-system :- BootstrapSystem
  "Create an instance of bootstrap system using a constructor which is
  specified via `:zou/constructor` key of the given config map."
  [conf :- BootstrapConfig]
  (let [ctor (:zou/constructor conf)
        ctor (if (symbol? ctor)
               (resolve-ctor ctor)
               ctor)
        conf (dissoc conf :zou/constructor)]
    (ctor conf)))

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
