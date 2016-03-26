(defproject zou/lib "0.1.0-alpha5-SNAPSHOT"
  :dependencies [[zou/common :version]
                 [zou/component :version]
                 [rkworks/cling "0.1.3" :exclusions [io.aviso/pretty]]]
  :plugins [[lein-modules "0.3.11"]])
