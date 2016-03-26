(defproject zou/lein-template "0.1.0-alpha4"
  :description "Leiningen template for Zou"
  :url "http://github.com/rfkm/zou"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  :plugins [[lein-modules "0.3.11"]]
  :deploy-repositories
  [["clojars-env" {:url "https://clojars.org/repo/"
                   :username [:gpg :env]
                   :password [:gpg :env]}]
   ^:replace ["snapshots" :clojars-env]
   ^:replace ["releases" :clojars-env]])
