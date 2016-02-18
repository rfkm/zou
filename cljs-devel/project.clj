(defproject zou/cljs-devel "0.1.0-alpha2"
  :dependencies [[zou/common :version]
                 [zou/web :version]
                 [figwheel-sidecar "0.5.0-6"]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:dev {:dependencies [[zou/framework :version]]}})
