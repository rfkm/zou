(ns zou.framework.repl
  (:refer-clojure :exclude [$])
  (:require [clojure.tools.namespace.repl :as tnr]
            [zou.framework.config :as conf]
            [zou.framework.core :as core]
            [zou.util.namespace :as ns]))

(declare inject-util-to-core)

(defn go []
  (inject-util-to-core)
  (core/boot-core!)
  (core/load-system (core/core) (conf/fetch-config-or-abort))
  (core/start-systems (core/core)))

(defn stop []
  (core/shutdown-core!))

(defn restart []
  (stop)
  (go))

(defn reset []
  (stop)
  (tnr/refresh :after 'zou.framework.repl/go))

(defn system [& ks]
  (get-in (core/system (core/core)) ks))

(def $ #'system)

(defn inject-util-to-core []
  (ns/inject clojure.core $))

(defn systems []
  (core/systems (core/core)))
