(ns zou.cljs.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            zou.specter-test
            zou.util.namespace-test
            zou.util.platform-test
            zou.util-test))

(doo-tests 'zou.specter-test
           'zou.util.namespace-test
           'zou.util.platform-test
           'zou.util-test)
