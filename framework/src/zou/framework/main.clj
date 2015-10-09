(ns zou.framework.main
  (:gen-class)
  (:require [cling.core :as cli]
            [clojure.java.io :as io]
            [zou.framework.core :as core]
            [zou.framework.env :as env]))

(def global-options
  [["-c" "--config PATH" "Config file"
    :parse-fn io/file
    :default (io/resource "zou/config/config.edn")
    :validate [#(.exists ^java.io.File %)  "File does not exist"
               #(.isFile ^java.io.File %)  "Path is not a regular file"
               #(.canRead ^java.io.File %) "File is unreadable"]
    :default-desc "resource://zou/config/config.edn"]])

(defn run [conf-source]
  (core/boot-core!)
  (core/load-systems (core/core) conf-source)
  (doseq [k (keys (core/systems (core/core)))]
    (core/start-system (core/core) k)))

(cli/defcmd run-cmd
  "Start up the system"
  []
  []
  [{:keys [options]}]
  (run (:config options))
  (cli/keep-alive))

(cli/defcontainer root
  global-options
  run-cmd)

(cli/defentrypoint -main
  root
  {:project-id "zou-framework"
   :exit-process? (env/in-prod?)})
