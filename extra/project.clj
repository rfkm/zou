(defproject zou/extra "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [zou/lib :version]
                 [inflections "0.13.0"]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles
  {:dev {:dependencies [[zou/framework :version]]}
   :provided {:dependencies [[ragtime "0.7.1"]]}})
