(ns zou.framework.repl
  (:refer-clojure :exclude [$])
  (:require [clojure.tools.namespace.repl :as tnr]
            [zou.framework.container :as container]
            [zou.framework.core :as core]
            [zou.util.namespace :as ns]))

(declare inject-util-to-core)

(defn go []
  (inject-util-to-core)
  (core/run-core (core/boot-core!)))

(defn stop []
  (core/shutdown-core!))

(defn restart []
  (stop)
  (go))

(defn reset []
  (stop)
  (tnr/refresh :after 'zou.framework.repl/go))

(defn system* [system-key & ks]
  (get-in (container/system (:container (core/core-system)) system-key) ks))

(defn system [& ks]
  ;; Assuming the system key is `:main`
  (apply system* :main ks))

(def $ #'system)

(defn inject-util-to-core []
  (ns/inject clojure.core $))

(defn systems []
  (container/systems (:container (core/core-system))))
