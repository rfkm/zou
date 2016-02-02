(ns zou.component-test
  (:require [clojure.test :as t]
            [com.stuartsierra.component :as c]
            [midje.sweet :refer :all]
            [zou.component :as sut]))

(defrecord FooComponent [])

(t/deftest ctor-test
  (fact "system-ctors"
    (#'sut/system-ctors {:a {:zou/constructor ..a-ctor.. :_ :_}
                         :b {:_ :_}})
    =>
    {:a ..a-ctor..
     :b identity}

    (#'sut/system-ctors {:a {:zou/constructor 'identity}})
    =>
    {:a #'identity}))

(t/deftest deps-test
  (fact "system-deps"
    (#'sut/system-deps {:c1 {:zou/constructor  identity
                             :zou/dependencies {:a :a :b' :b}
                             }
                        :c2 {:zou/constructor identity}})
    =>
    {:c1 {:a  :a
          :b' :b}}))

(t/deftest conf-test
  (fact "system-confs"
    (#'sut/system-confs {:c1 {:a :a}
                         :c2 {:zou/constructor identity
                              :b               :b}})
    =>
    {:c1 {:a :a}
     :c2 {:b :b}}))

(t/deftest dependants-test
  (fact "translate-dependants"
    (#'sut/translate-dependants {:c1 {:zou/dependencies {:a :a :b' :b}
                                      :zou/dependants   {:c3 :c1'}}
                                 :c2 {}
                                 :c3 {:zou/dependencies {:c4' :c4}
                                      :zou/dependants   {:c2 :c3'}}
                                 :c4 {}})
    =>
    {:c1 {:zou/dependencies {:a :a :b' :b}}
     :c2 {:zou/dependencies {:c3' :c3}}
     :c3 {:zou/dependencies {:c1' :c1 :c4' :c4}}
     :c4 {}}))

(t/deftest optionals-test
  (fact "translate-optionals"
    (#'sut/translate-optionals {:c1           {:zou/optionals {:a :non-existent-key
                                                               :b :existent-key}}
                                :existent-key {:a :b}})
    =>
    {:c1           {:zou/dependencies {:b :existent-key}}
     :existent-key {:a :b :zou/dependencies {}}}))

(t/deftest tags-test
  (fact "translate-tags"
    (#'sut/translate-tags {:c1 {:zou/tags [:tag1 [:tag2 :c1']] :a :b}
                           :c2 {:zou/tags [:tag1 [:tag2 :c2']] :a :b}
                           :c3 {:zou/dependencies {:tagged :tag1}}
                           :c4 {:zou/dependencies {:tagged :tag2}}})
    =>
    {:c1   {:a :b}
     :c2   {:a :b}
     :c3   {:zou/dependencies {:tagged :tag1}}
     :c4   {:zou/dependencies {:tagged :tag2}}
     :tag1 {:zou/dependencies {:c1 :c1 :c2 :c2}}
     :tag2 {:zou/dependencies {:c1' :c1 :c2' :c2}}}

    (#'sut/translate-tags {:c1 {:zou/tags [:tag1]} :tag1 {:a :b}})
    => {:c1   {}
        :tag1 {:zou/dependencies {:c1 :c1} :a :b}}))

(t/deftest build-system-map-test
  (fact "build-system-map"
    (sut/build-system-map {:c1 {:a :a}
                           :c2 {:zou/constructor  map->FooComponent
                                :zou/dependencies {:c1 :c1}
                                :zou/dependants   {:c7 :c2'}
                                :b                :b}
                           :c3 [:a :b]
                           :c4 false                ; should be ignored
                           :c5 nil                  ; should be ignored
                           :c6 {:zou/disabled true} ; should be ignored
                           :c7 {}})
    =>
    ..system-map+deps..
    (provided
     (map->FooComponent {:b :b}) => ..foo..
     (c/map->SystemMap {:c1 {:a :a} :c2 ..foo.. :c3 [:a :b] :c7 {}}) => ..system-map..
     (c/system-using ..system-map.. {:c1 {} :c2 {:c1 :c1} :c7 {:c2' :c2}}) => ..system-map+deps..)

    (sut/build-system-map {:c1 {:zou/tags [:tag1 [:tag2 :c1']]}
                           :c2 {:zou/dependencies {:tagged :tag1}}
                           :c3 {:zou/dependencies {:tagged :tag2}}})
    => ..system-map+deps..
    (provided
     (c/map->SystemMap {:c1 {} :c2 {} :c3 {} :tag1 {} :tag2 {}}) => ..system-map..
     (c/system-using ..system-map.. {:c1   {}
                                     :c2   {:tagged :tag1}
                                     :c3   {:tagged :tag2}
                                     :tag1 {:c1 :c1}
                                     :tag2 {:c1' :c1}})
     =>
     ..system-map+deps..)))

(t/deftest subsystem-test
  (fact "extract-subsystem-conf"
    (let [conf {:c1 {:a                :a
                     :zou/dependencies {:c2' :c2}}
                :c2 {:a                :a
                     :zou/dependencies {:c3' :c3}}
                :c3 {:a :a}}]
      (set (keys (#'sut/extract-subsystem-conf conf [:c3]))) => #{:c3}
      (set (keys (#'sut/extract-subsystem-conf conf [:c2]))) => #{:c2 :c3}
      (set (keys (#'sut/extract-subsystem-conf conf [:c1]))) => #{:c1 :c2 :c3})))

(t/deftest utils-test
  (fact "with-component"
    (sut/with-component [c (->FooComponent)] c) => ..foo'..
    (provided
     (->FooComponent) => ..foo..
     (sut/start ..foo..) => ..foo'..
     (sut/stop ..foo'..) => ..foo''..))

  (fact "with-component w/ error"
    (sut/with-component [c (->FooComponent)]
      (throw (Error. "error")))
    =>
    (throws "error")
    (provided
     (->FooComponent) => ..foo..
     (sut/start ..foo..) => ..foo'..
     (sut/stop ..foo'..) => ..foo''..))

  (fact "with-system"
    (sut/with-system [s ..conf..] s) => ..sys'..
    (provided
     (sut/build-system-map ..conf..) => ..sys..
     (sut/start ..sys..) => ..sys'..
     (sut/stop ..sys'..) => ..sys''..))

  (fact "with-system w/ subsystem keys"
    (sut/with-system [s ..conf.. :a :b] s) => ..sys'..
    (provided
     (sut/build-system-map ..conf.. [:a :b]) => ..sys..
     (sut/start ..sys..) => ..sys'..
     (sut/stop ..sys'..) => ..sys''..)))
