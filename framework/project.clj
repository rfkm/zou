(defproject zou/framework "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [zou/lib :version]
                 [rkworks/baum "0.4.0"]
                 [environ "1.1.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:dev {:source-paths ["dev"]
                   :main zou.framework.main}}
  :repl-options {:init-ns zou.framework.repl})
