(ns zou.framework.core
  (:require [zou.framework.bootstrap :as boot]
            [zou.framework.config :as conf]
            [zou.framework.entrypoint.proto :as ep]
            [zou.logging :as log]))

(defonce ^:private +bootstrap-system+ nil)

(defn boot!
  "Create an instance of bootstrap system with the given config map, and
  start its life cycle. If the config map is not specified, it will be
  fetched via `zou.framework.config/fetch-config-or-abort`.

  Also, the started instance will be bound to the given var. If the
  var is not specified, `#'zou.framework.core/+bootstrap-system+` will be
  used. Note that an old instance bound to the given var will be
  stopped before creating a new instance if exists."
  ([] (boot! (conf/read-config (conf/fetch-config-or-abort))))
  ([conf] (boot! #'+bootstrap-system+ conf))
  ([core-var conf]
   (alter-var-root
    core-var
    (fn [old]
      (let [boot #(boot/init-bootstrap-system (boot/make-bootstrap-system conf))]
        (if (nil? old)
          (boot)
          (do
            (log/warn "Bootstrap system is already running. Rebooting...")
            (boot/deinit-bootstrap-system old)
            (boot))))))))

(defn shutdown!
  "Stop the bootstrap system bound to the given var. If the var is not
  specified, `#'zou.framework.core/+bootstrap-system+` will be used."
  ([] (shutdown! #'+bootstrap-system+))
  ([core-var]
   (alter-var-root
    core-var
    (fn [sys]
      (when sys
        (boot/deinit-bootstrap-system sys))
      nil))))

(defn run
  "Run the given bootstrap system with `zou.framework.entrypoint.proto/EntryPoint#run`.
  The bootstrap system will delegate its process to an entry point
  component that is a member of it. By default,
  `zou.framework.entrypoint.impl.DefaultEntryPoint` is assinged to the
  etnry point component."
  [bootstrap-system & args]
  (ep/run bootstrap-system args))

(defn bootstrap-system
  "Return the current instance of bootstrap system. You should use this only
  for debugging purposes or for convenience during development. Note
  that this works only when the instance is bound to
  `#'zou.framework.core/+bootstrap-system+`."
  []
  (when (nil? +bootstrap-system+)
    (throw (RuntimeException. "Failed to find a bootstrap system. Did you forget to start it?")))
  +bootstrap-system+)
