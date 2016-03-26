(defproject zou/common "0.1.0-alpha4"
  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [spootnik/unilog "0.7.13"]
                 [io.aviso/pretty "0.1.24"]
                 [prismatic/schema "1.0.5"]
                 [bultitude "0.2.8"]
                 [im.chit/hara.namespace "2.2.17"]
                 [potemkin "0.4.3"]
                 [prismatic/plumbing "0.5.2"]
                 [medley "0.7.3"]
                 [com.rpl/specter "0.9.2" :exclusions [org.clojure/clojurescript]]]
  :plugins [[lein-modules "0.3.11"]])
