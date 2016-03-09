(ns zou.framework.repl
  (:refer-clojure :exclude [$])
  (:require [clojure.tools.namespace.repl :as tnr]
            [zou.framework.container :as container]
            [zou.framework.core :as core]
            [zou.util.namespace :as ns]))

(declare inject-util-to-core)

(defn go []
  (inject-util-to-core)
  (core/run (core/boot!)))

(defn stop []
  (core/shutdown!))

(defn restart []
  (stop)
  (go))

(defn reset []
  (stop)
  (tnr/refresh :after 'zou.framework.repl/go))

(defn system [& ks]
  (get-in (container/system (:container (core/bootstrap-system))) ks))

(def $ #'system)

(defn inject-util-to-core []
  (ns/inject clojure.core $))
