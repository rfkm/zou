(def zou-version "0.1.0-alpha5-SNAPSHOT")

(defproject zou-todo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.5.2"
  :repositories [["zou-repo" "https://s3.amazonaws.com/zou-repo"]]
  :uberjar-name "zou-todo.jar"
  :repl-options {:init-ns zou.framework.repl}
  :source-paths ["src" "src-cljs"]
  :resource-paths ["resources" "target/sassc" "target/cljs"]
  :target-path "target/%s"
  :exclusions   [org.clojure/clojurescript]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [zou ~zou-version]
                 [com.h2database/h2 "1.4.191"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [stencil "0.5.0"]
                 [selmer "1.0.0"]
                 [hiccup "1.0.5"]]
  :profiles {:provided {:dependencies [[org.clojure/clojurescript "1.7.228"]
                                       [zou/devel ~zou-version]]}
             :repl     {:dependencies [[com.cemerick/piggieback "0.2.1"]]}
             :uberjar  {:prep-tasks ["javac" "compile"
                                     ["run" "sassc" "compile"]
                                     ["run" "cljs" "compile"]]
                        :aot        [zou.framework.main #".*"]}
             :dev      {:jvm-opts     ["-Dzou-env=dev"]
                        :dependencies [[midje "1.8.3"]]
                        :plugins      [[lein-midje "3.2"]]}}
  :main zou.framework.main)
