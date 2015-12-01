(ns zou.framework.config-test
  (:require [baum.core :as b]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.framework.config :as sut]
            [zou.framework.env :as env]))

(defn read-with-reader [reader s]
  (b/read-string {:readers {'sut reader}}
                 s))

(t/deftest when-reader-test
  (fact "when-dev-reader"
    (env/with-dev-env
      (read-with-reader sut/when-dev-reader "#sut :a")) => :a

    (env/with-prod-env
      (read-with-reader sut/when-dev-reader "#sut :a")) => nil)

  (fact "when-prod-reader"
    (env/with-prod-env
      (read-with-reader sut/when-prod-reader "#sut :a")) => :a

    (env/with-dev-env
      (read-with-reader sut/when-prod-reader "#sut :a")) => nil)

  (fact "when-test-reader"
    (env/with-test-env
      (read-with-reader sut/when-test-reader "#sut :a")) => :a

    (env/with-dev-env
      (read-with-reader sut/when-prod-reader "#sut :a")) => nil))

(t/deftest if-reader-test
  (fact "if-dev-reader"
    (env/with-dev-env
      (read-with-reader sut/if-dev-reader "#sut [:a :b]")) => :a

    (env/with-prod-env
      (read-with-reader sut/if-dev-reader "#sut [:a :b]")) => :b)

  (fact "if-prod-reader"
    (env/with-prod-env
      (read-with-reader sut/if-prod-reader "#sut [:a :b]")) => :a

    (env/with-dev-env
      (read-with-reader sut/if-prod-reader "#sut [:a :b]")) => :b)

  (fact "if-test-reader"
    (env/with-test-env
      (read-with-reader sut/if-test-reader "#sut [:a :b]")) => :a

    (env/with-dev-env
      (read-with-reader sut/if-prod-reader "#sut [:a :b]")) => :b))

(t/deftest cond-reader-test
  (fact "env-cond"
    (env/with-dev-env
      (read-with-reader sut/env-cond-reader "#sut {:dev :a :prod :b :test :c}") => :a)

    (env/with-prod-env
      (read-with-reader sut/env-cond-reader "#sut {:dev :a :prod :b :test :c}") => :b)

    (env/with-test-env
      (read-with-reader sut/env-cond-reader "#sut {:dev :a :prod :b :test :c}") => :c)

    (env/with-app-env :foo
      (read-with-reader sut/env-cond-reader "#sut {:dev :a :prod :b :test :c}") => nil)

    (env/with-app-env :foo
      (read-with-reader sut/env-cond-reader "#sut {:dev :a :prod :b :test :c :else :d}") => :d)))
