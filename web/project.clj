(defproject zou/web "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [zou/lib :version]
                 [org.immutant/web "2.1.5"]
                 [org.projectodd.wunderboss/wunderboss-core "0.12.2"
                  :exclusions [org.jboss.logging/jboss-logging]] ; for suppressing pedantic warning
                 [ring/ring-core "1.5.0" :exclusions [org.clojure/tools.reader]]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-devel "1.5.0"]
                 [metosin/ring-http-response "0.8.0"]
                 [ring-logger "0.7.6"]
                 [ring.middleware.conditional "0.2.0"]
                 [ring-webjars "0.1.1" :exclusions [com.fasterxml.jackson.core/jackson-databind]]
                 [bidi "2.0.12"]
                 [com.cemerick/url "0.1.1" :exclusions [pathetic]]
                 [pathetic "0.5.1"]
                 [prone "1.1.2"]
                 [hawk "0.2.10"]]
  :plugins [[lein-modules "0.3.11"]])
