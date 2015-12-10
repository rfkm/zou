(def zou-version "0.1.0-SNAPSHOT")

(defproject zou-todo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.5.2"
  :clean-targets ^{:protect false} [:target-path "resources/public/js/dist" "out"]
  :repositories [["zou-repo"
                  {:url "https://s3.amazonaws.com/zou-repo"}]]
  :uberjar-name "zou-todo.jar"
  :repl-options {:init-ns zou.framework.repl
                 :init (zou.framework.core/boot-core!)}
  :source-paths ["src" "src-cljs"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [zou ~zou-version]
                 [com.h2database/h2 "1.4.190"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [stencil "0.5.0"]
                 [selmer "0.9.5"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-environ "1.0.1"]
            [rkworks/lein-baum "0.1.0"]
            [lein-cljsbuild "1.1.1"]]
  :cljsbuild {import "resources/zou/config/cljsbuild.edn"}
  :profiles {:uberjar {:env {:prod true}
                       :aot :all
                       :hooks [leiningen.cljsbuild]}
             :dev {:env {:zou-env :dev}
                   :dependencies [[zou/devel ~zou-version]
                                  [midje "1.8.2"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :plugins [[lein-midje "3.2"]]}}
  :main zou-todo.main)
