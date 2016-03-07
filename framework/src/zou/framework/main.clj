(ns zou.framework.main
  (:gen-class)
  (:require [zou.framework.cli :as cli]
            [zou.framework.config :as conf]
            [zou.framework.core :as core]))

(defn -main
  [& args]
  (let [core (-> args
                 cli/extract-config-file
                 conf/read-config
                 core/boot-core!)]
    (apply core/run-core core args)))
