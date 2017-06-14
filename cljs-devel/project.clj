(defproject zou/cljs-devel "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/web :version :exclusions [hawk]]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [figwheel-sidecar "0.5.10" :exclusions [com.stuartsierra/component
                                                         org.clojure/clojurescript]]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:dev {:dependencies [[zou/framework :version :exclusions [org.apache.commons/commons-compress org.clojure/tools.reader]]

                                  ;; for suppressing pedantic warning
                                  [ring/ring-mock "0.3.0" :exclusions [ring/ring-codec]]
                                  [stencil "0.5.0" :exclusions [org.clojure/core.cache]]]}})
