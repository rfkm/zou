(def modules ["common" "component" "framework" "web" "db" "cljs-devel" "devel"])
(def meta-modules ["devel"])
(def non-meta-modules (remove (set meta-modules) modules))

(defproject zou "0.1.0-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/framework :version]
                 [zou/web :version]
                 [zou/db :version]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:coverage {:plugins      [[lein-cloverage "1.0.6"]
                                       [lein-exec "0.3.5"]
                                       [cloverage "1.0.6" :exclusions [org.clojure/tools.cli]]
                                       [org.clojure/tools.cli "0.3.3"]
                                       [lein-shell "0.4.2"]]
                        :dependencies [[cheshire "5.5.0"]]}
             :dev {:dependencies [[midje "1.8.2"]
                                  [org.clojure/clojure "1.7.0"]
                                  [clj-http "2.0.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [org.clojure/java.jdbc "0.4.2"]
                                  [org.postgresql/postgresql "9.4-1204-jdbc42"]
                                  [com.h2database/h2 "1.4.190"]
                                  [enlive "1.1.6"]
                                  [hiccup "1.0.5"]
                                  [stencil "0.5.0"]
                                  [selmer "0.9.5"]]
                   :plugins      [[lein-midje "3.2"]]
                   :env          {:zou-env :dev}
                   :aliases      {"coverage-sub" ["do"
                                                  ["cloverage" "--coveralls"]
                                                  ["exec" "-p" "../etc/coverage.clj"]]
                                  "coverage-all" ~(into ["exec" "-p" "etc/coverage_all.clj"] non-meta-modules)
                                  "coverage"     ["with-profile" "+coverage"
                                                  "do"
                                                  ["modules" ":dirs" ~(clojure.string/join ":" non-meta-modules)
                                                   "coverage-sub"]
                                                  ["coverage-all"]]
                                  "coveralls"    ["with-profile" "+coverage"
                                                  "shell" "curl" "-F"
                                                  "json_file=@coveralls.json"
                                                  "https://coveralls.io/api/v1/jobs"]}}}
  :modules {:dirs ~modules
            :subprocess nil
            :inherited
            {:url                 "https://github.com/rfkm/zou"
             :description         "Component-based framework"
             :repositories        [["zou-repo"
                                    {:url "https://s3.amazonaws.com/zou-repo"}]]
             :deploy-repositories [["zou-repo" {:url        "s3p://zou-repo/"
                                                :username   :env/aws_access_key_id
                                                :passphrase :env/aws_secret_access_key}]]
             :license             {:name "Eclipse Public License"
                                   :url "http://www.eclipse.org/legal/epl-v10.html"}
             :scm                 {:dir ".."}
             :plugins             [[lein-environ "1.0.1"]
                                   [s3-wagon-private "1.2.0"]]}})
