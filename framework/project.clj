(defproject zou/framework "0.1.0-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [rkworks/baum "0.3.0"]
                 [environ "1.0.2"]
                 [rkworks/cling "0.1.1"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:dev {:source-paths ["dev"]
                   :main zou.framework.main}}
  :repl-options {:init-ns zou.framework.repl})
