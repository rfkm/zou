(ns zou.component.parser.scanner-test
  (:require [clojure.test :as t]
            [zou.component.parser :as p]
            [zou.component.parser.scanner :as sut]
            [zou.util.namespace :as un]))

(t/deftest parse-system-config-entry-test
  (un/with-temp-ns [ns "foo.bar.baz" '((defn make-my-component
                                         {:zou/component {:zou/name         :fooo
                                                          :zou/tags         [:tag/foo :tag/bar]
                                                          :zou/dependencies {:counter' :counter}}}
                                         [conf]
                                         {:c conf}))]
    (t/is (= (p/parse-system-config-entry {} :zou/component-scan {:prefixes ["foo.bar"]})
             {:components
              {:fooo {:dependencies {:counter' :counter}
                      :constructor (ns-resolve ns 'make-my-component)},
               :tag/foo {:dependencies {:fooo :fooo}}
               :tag/bar {:dependencies {:fooo :fooo}}}}))))
