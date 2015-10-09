(ns zou.framework.repl
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tnr]
            [zou.framework.core :as core]))

(def config-file-path "zou/config/config.edn")

(defn go []
  (core/boot-core!)
  (core/load-systems (core/core) (io/resource config-file-path))
  (doseq [k (keys (core/systems (core/core)))]
    (core/start-system (core/core) k)))

(defn stop []
  (core/shutdown-core!))

(defn restart []
  (stop)
  (go))

(defn reset []
  (stop)
  (tnr/refresh :after 'zou.framework.repl/go))

(defn system [& ks]
  (get-in (core/systems (core/core)) ks))

(defn systems []
  (core/systems (core/core)))
