(ns zou.framework.repl
  (:refer-clojure :exclude [$])
  (:require [clojure.tools.namespace.repl :as tnr]
            [zou.component :as c]
            [zou.framework.bootstrap :as boot]
            [zou.framework.cli :as cli]
            [zou.framework.config :as conf]
            [zou.framework.container :as container]
            [zou.framework.core :as core]
            [zou.framework.entrypoint.proto :as ep]
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

(defn exec
  "Invoke CLI interface with the given arguments.

  E.g.
    To print help message, eval the following sexp:
      (exec \"-h\")

  NB:
  Note that this will temporarily create a new bootstrap system. It
  means components which depend on external resources (e.g. port)
  might conflict with existing one."
  [& args]
  (c/with-component [c (-> args
                           cli/extract-config-file
                           conf/read-config
                           boot/make-bootstrap-system)]
    (ep/run c args)))

(defn system [& ks]
  (get-in (container/system (:container (core/bootstrap-system))) ks))

(def $ #'system)

(defmacro $-> [component-key & forms]
  `(-> (system ~component-key)
       ~@forms))

(defmacro $->> [component-key & forms]
  `(->> (system ~component-key)
        ~@forms))

(defn inject-util-to-core []
  (ns/inject clojure.core $)
  (ns/inject clojure.core $->)
  (ns/inject clojure.core $->>))
