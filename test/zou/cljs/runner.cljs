(ns zou.cljs.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [zou.util.namespace-test]
            [zou.util.platform-test]))

(doo-tests 'zou.util.namespace-test
           'zou.util.platform-test)
