(defproject zou/devel "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/cljs-devel :version]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:dev {:dependencies [;; for suppressing pedantic warning
                                  [ring/ring-mock "0.3.0" :exclusions [ring/ring-codec]]
                                  [stencil "0.5.0" :exclusions [org.clojure/core.cache]]]}})
