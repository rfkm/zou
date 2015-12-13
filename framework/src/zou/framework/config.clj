(ns zou.framework.config
  (:require [baum.core :as b]
            [clojure.java.io :as io]
            [environ.core :as environ]
            [zou.framework.env :as env]))

(def default-config-path "zou/config/config.edn")

(def config-env-key :zou-config)

(defn- file-or-resource [path]
  (or
   (let [f (io/file path)] (when (.exists f) f))
   (io/resource path)))

(defn fetch-config
  "Finds a config file."
  []
  (file-or-resource (or (:zou-config environ/env)
                        default-config-path)))

(defn fetch-config-or-abort
  "Like `fetch-config` but throws an exception when no config file is found."
  []
  (or (fetch-config)
      (throw (RuntimeException. "Found no config file"))))

(b/deflazyreader when-dev-reader [v opts]
  (b/reduction (b/if-reader [(env/in-dev?)
                             v])
               opts))

(b/deflazyreader when-prod-reader [v opts]
  (b/reduction (b/if-reader [(env/in-prod?)
                             v])
               opts))

(b/deflazyreader when-test-reader [v opts]
  (b/reduction (b/if-reader [(env/in-test?)
                             v])
               opts))

(b/deflazyreader if-dev-reader [[then else] opts]
  (b/reduction (b/if-reader [(env/in-dev?)
                             then
                             else])
               opts))

(b/deflazyreader if-prod-reader [[then else] opts]
  (b/reduction (b/if-reader [(env/in-prod?)
                             then
                             else])
               opts))

(b/deflazyreader if-test-reader [[then else] opts]
  (b/reduction (b/if-reader [(env/in-test?)
                             then
                             else])
               opts))

(b/deflazyreader env-cond-reader [clauses opts]
  (b/reduction (b/match-reader (into [(env/app-env)] (concat (apply concat (dissoc clauses :else))
                                                             [:else (:else clauses)])))
               opts))

(defn read-config [file]
  (b/read-config file
                 {:shorthand? true
                  :readers {'zou/when-dev when-dev-reader
                            'zou/when-prod when-prod-reader
                            'zou/when-test when-test-reader
                            'zou/if-dev if-dev-reader
                            'zou/if-prod if-prod-reader
                            'zou/if-test if-test-reader
                            'zou/cond env-cond-reader}}))
