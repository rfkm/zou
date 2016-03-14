(def zou-version "0.1.0-alpha4-SNAPSHOT")

(defproject {{raw-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.5.2"
  :repositories [["zou-repo" "https://s3.amazonaws.com/zou-repo"]]
  :uberjar-name "{{uberjar-name}}"
  :repl-options {:init-ns zou.framework.repl}{{#cljs?}}
  :source-paths ["src" "src-cljs"]{{/cljs?}}
  :resource-paths ["resources"{{#sassc?}} "target/sassc"{{/sassc?}}{{#cljs?}} "target/cljs"{{/cljs?}}]
  :target-path "target/%s"
  :exclusions   [org.clojure/clojurescript]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [zou ~zou-version]{{#h2?}}
                 [com.h2database/h2 "1.4.191"]{{/h2?}}{{#postgresql?}}
                 [org.postgresql/postgresql "9.4.1208"]{{/postgresql?}}{{#mysql?}}
                 [mysql/mysql-connector-java "5.1.38"]{{/mysql?}}{{#db?}}
                 [org.clojure/java.jdbc "0.4.2"]{{/db?}}{{#enlive?}}
                 [enlive "1.1.6"]{{/enlive?}}{{#hiccup?}}
                 [hiccup "1.0.5"]{{/hiccup?}}]
  :plugins [{{#midje?}}[lein-midje "3.2"]{{/midje?}}]
  :profiles {:provided {:dependencies [[zou/devel ~zou-version{{^cljs?}} :exclusions [zou/cljs-devel]{{/cljs?}}]{{#cljs?}}
                                       [org.clojure/clojurescript "1.7.228"]{{/cljs?}}]}{{#cljs?}}
             :repl     {:dependencies [[com.cemerick/piggieback "0.2.1"]]}{{/cljs?}}
             :uberjar  {:aot        [zou.framework.main #".*"]
                        :prep-tasks ["javac" "compile"{{#sassc?}}
                                     ["run" "sassc" "compile"]{{/sassc?}}{{#cljs?}}
                                     ["run" "cljs" "compile"]{{/cljs?}}]}
             :dev      {:jvm-opts     ["-Dzou-env=dev"]
                        :dependencies [{{#midje?}}[midje "1.8.3"]{{/midje?}}]}}
  :main zou.framework.main)
