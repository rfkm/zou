(def zou-version "0.1.0-alpha3-SNAPSHOT")

(defproject {{raw-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.5.2"{{#cljs?}}
  :clean-targets ^{:protect false} [:target-path "resources/public/js/dist" "out"]
  :source-paths ["src" "src-cljs"]{{/cljs?}}
  :repositories [["zou-repo"
                  {:url "https://s3.amazonaws.com/zou-repo"}]]
  :uberjar-name "{{uberjar-name}}"
  :repl-options {:init-ns zou.framework.repl
                 :init (zou.framework.core/boot!)}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [zou ~zou-version]{{#h2?}}
                 [com.h2database/h2 "1.4.190"]{{/h2?}}{{#postgresql?}}
                 [org.postgresql/postgresql "9.4-1205-jdbc41"]{{/postgresql?}}{{#mysql?}}
                 [mysql/mysql-connector-java "5.1.37"]{{/mysql?}}{{#db?}}
                 [org.clojure/java.jdbc "0.4.2"]{{/db?}}{{#cljs?}}
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]{{/cljs?}}{{#enlive?}}
                 [enlive "1.1.6"]{{/enlive?}}{{#hiccup?}}
                 [hiccup "1.0.5"]{{/hiccup?}}]
  :plugins [[lein-environ "1.0.1"]{{#cljs?}}
            [rkworks/lein-baum "0.1.0"]
            [lein-cljsbuild "1.1.1"]{{/cljs?}}]{{#cljs?}}
  :cljsbuild {import "resources/zou/config/cljsbuild.edn"}{{/cljs?}}
  :profiles {:uberjar {:env {:prod true}
                       :aot [zou.framework.main #".*"]{{#cljs?}}
                       :hooks [leiningen.cljsbuild]{{/cljs?}}}
             :dev {:env {:zou-env "dev"}
                   :dependencies [[zou/devel ~zou-version{{^cljs?}} :exclusions [zou/cljs-devel]{{/cljs?}}]{{#midje?}}
                                  [midje "1.8.2"]{{/midje?}}{{#cljs?}}
                                  [com.cemerick/piggieback "0.2.1"]{{/cljs?}}]
                   :plugins [{{#midje?}}[lein-midje "3.2"]{{/midje?}}]}}
  :main zou.framework.main)
