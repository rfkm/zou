(ns zou.framework.config
  (:require [baum.core :as b]
            [zou.framework.env :as env]))


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
  (let [clauses (if-not (contains? clauses :else)
                  (assoc clauses :else nil)
                  clauses)]
    (b/reduction (b/match-reader (into [(env/app-env)] (apply concat clauses)))
                 opts)))

(defn read-config [file]
  (b/read-config file
                 {:shorthand? true
                  :readers {'zou/when-dev when-dev-reader
                            'zou/when-prod when-prod-reader
                            'zou/when-test when-test-reader
                            'zou/if-dev if-dev-reader
                            'zou/if-prod if-prod-reader
                            'zou/if-test if-test-reader
                            :zou/cond env-cond-reader}}))
