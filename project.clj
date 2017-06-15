(def modules ["common" "component" "lib" "framework" "web" "db" "extra" "cljs-devel" "devel"])
(def modules+tpl (conj modules "lein-template"))
(def modules+tpl+parent (conj modules+tpl "."))

(defn subdir [path]
  (mapv #(str % "/" path) modules))

(defproject zou "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/lib :version]
                 [zou/framework :version]
                 [zou/web :version :exclusions [org.apache.commons/commons-compress]]
                 [zou/db :version]
                 [zou/extra :version]]
  :plugins [[lein-modules "0.3.11"]]
  :profiles {:coverage {:source-paths   ~(subdir "src")
                        :test-paths     ~(subdir "test")
                        :resource-paths ~(into (subdir "dev-resources")
                                               (subdir "resources"))
                        :plugins        [[rfkm/lein-cloverage "1.0.9-SNAPSHOT"]
                                         [lein-exec "0.3.6"]]
                        :dependencies   [[zou/devel :version]
                                         [cheshire "5.7.1"]
                                         [org.clojure/tools.cli "0.3.5"]]}
             :deploy   {:deploy-repositories [["zou-repo" {:url        "s3p://zou-repo/"
                                                           :username   [:gpg :env/aws_access_key_id]
                                                           :passphrase [:gpg :env/aws_secret_access_key]}]
                                              ["releases" :zou-repo]
                                              ["snapshots" :zou-repo]]
                        :plugins             [[s3-wagon-private "1.2.0"]]}
             :cljs-test {:source-paths   ~(subdir "src")
                         :test-paths     ~(subdir "test")
                         :plugins [[lein-doo "0.1.7"]]
                         :cljsbuild {:builds [{:id "test"
                                               :source-paths ~(vec (concat
                                                                    (subdir "src")
                                                                    (subdir "test")
                                                                    ["test"]))
                                               :compiler {:output-to "resources/public/js/dist/testable.js"
                                                          :main zou.cljs.runner
                                                          :optimizations :none}}]}}
             :dev      {:dependencies [[org.clojure/clojurescript "1.9.229" :exclusions [org.clojure/tools.reader]]
                                       [midje "1.8.3"]
                                       [org.clojure/clojure "1.8.0"]
                                       [clj-http "3.6.1"]
                                       [ring/ring-mock "0.3.0"]
                                       [org.clojure/java.jdbc "0.6.1"]
                                       [org.postgresql/postgresql "42.1.1"]
                                       [com.h2database/h2 "1.4.196"]
                                       [enlive "1.1.6"]
                                       [hiccup "1.0.5"]
                                       [stencil "0.5.0"]
                                       [selmer "1.10.7"]
                                       [ragtime "0.7.1"]]
                        :plugins      [[lein-midje "3.2.1"]
                                       [lein-file-replace "0.1.0"]]
                        :env          {:zou-env "dev"}
                        :aliases      {"coverage" ["with-profile" "+coverage" "do"
                                                   ["cloverage" "--codecov"
                                                    "-r" "midje"]
                                                   ["exec" "-p" "etc/codecov.clj"]]
                                       "modules+" ["modules" ":dirs" ~(clojure.string/join "," modules+tpl)]
                                       "modules++" ["modules" ":dirs" ~(clojure.string/join "," modules+tpl+parent)]
                                       "bump-template-zou-version"
                                       ["file-replace"
                                        "lein-template/resources/leiningen/new/zou/project.clj"
                                        "zou-version \""
                                        "\""
                                        "version"]
                                       "bump-example-zou-version"
                                       ["file-replace"
                                        "example/project.clj"
                                        "zou-version \""
                                        "\""
                                        "version"]
                                       "bump-readme-zou-version"
                                       ["file-replace"
                                        "README.md"
                                        "zou \""
                                        "\"]"
                                        "version"]}}}
  :modules {:dirs       ~modules
            :subprocess nil
            :inherited
            {:url          "https://github.com/rfkm/zou"
             :description  "Component-based framework"
             :repositories [["zou-repo"
                             {:url "https://s3.amazonaws.com/zou-repo"}]]
             :license      {:name "Eclipse Public License"
                            :url  "http://www.eclipse.org/legal/epl-v10.html"}
             :scm          {:dir ".."}
             :pedantic? :warn
             :plugins      [[lein-environ "1.0.2"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["modules++" "change" "version" "leiningen.release/bump-version" "release"]
                  ["bump-template-zou-version"]
                  ["bump-readme-zou-version"]
                  ["bump-example-zou-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["with-profile" "+deploy" "modules++" "deploy"]
                  ["modules++" "change" "version" "leiningen.release/bump-version"]
                  ["bump-template-zou-version"]
                  ["bump-readme-zou-version"]
                  ["bump-example-zou-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
