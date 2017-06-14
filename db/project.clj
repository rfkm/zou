(defproject zou/db "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [hikari-cp "1.7.5" :exclusions [org.slf4j/slf4j-api]]
                 [funcool/cats "2.1.0"]]
  :plugins [[lein-modules "0.3.11"]])
