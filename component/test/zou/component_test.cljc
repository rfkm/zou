(ns zou.component-test
  (:require [com.stuartsierra.component :as c]
            [zou.component :as sut :include-macros true]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true]))
  #?(:clj (:import (clojure.lang ExceptionInfo))))

(defrecord FooComponent [])
(defrecord StatefulComponent [state]
  c/Lifecycle
  (start [this]
    (swap! state conj :started)
    (assoc this :started true))
  (stop [this]
    (swap! state conj :stopped)
    (assoc this :stopped true)))

(defrecord BrokenComponent []
  c/Lifecycle
  (start [this]
    (throw (ex-info "error" {:this this})))
  (stop [this]
    this))

(defn new-stateful-component [_]
  (->StatefulComponent (atom [])))

(t/deftest ctor-test
  (let [ctor (fn [m])]
    (t/is (= (#'sut/system-ctors {:a {:zou/constructor ctor :_ :_}
                                  :b {:_ :_}})
             {:a ctor
              :b identity})))

  (t/testing "resolving"
    #?(:clj
       (t/is (= (#'sut/system-ctors {:a {:zou/constructor 'identity}})
                {:a #'identity}))

       :cljs
       ;; Resolving feature doesn't support CLJS
       (t/is (thrown? ExceptionInfo (#'sut/system-ctors {:a {:zou/constructor 'identity}}))))))

(t/deftest deps-test
  (t/is
   (= (#'sut/system-deps {:c1 {:zou/constructor  identity
                               :zou/dependencies {:a :a :b' :b}
                               }
                          :c2 {:zou/constructor identity}})
      {:c1 {:a  :a
            :b' :b}})))

(t/deftest conf-test
  (t/is
   (= (#'sut/system-confs {:c1 {:a :a}
                           :c2 {:zou/constructor identity
                                :b               :b}})
      {:c1 {:a :a}
       :c2 {:b :b}})))

(t/deftest dependants-test
  (t/is
   (= (#'sut/translate-dependants {:c1 {:zou/dependencies {:a :a :b' :b}
                                        :zou/dependants   {:c3 :c1'}}
                                   :c2 {}
                                   :c3 {:zou/dependencies {:c4' :c4}
                                        :zou/dependants   {:c2 :c3'}}
                                   :c4 {}})

      {:c1 {:zou/dependencies {:a :a :b' :b}}
       :c2 {:zou/dependencies {:c3' :c3}}
       :c3 {:zou/dependencies {:c1' :c1 :c4' :c4}}
       :c4 {}})))

(t/deftest optionals-test
  (t/is
   (= (#'sut/translate-optionals {:c1           {:zou/optionals {:a :non-existent-key
                                                                 :b :existent-key}}
                                  :existent-key {:a :b}})
      {:c1           {:zou/dependencies {:b :existent-key}}
       :existent-key {:a :b :zou/dependencies {}}})))

(t/deftest tags-test
  (t/is
   (= (#'sut/translate-tags {:c1 {:zou/tags [:tag1 [:tag2 :c1']] :a :b}
                             :c2 {:zou/tags [:tag1 [:tag2 :c2']] :a :b}
                             :c3 {:zou/dependencies {:tagged :tag1}}
                             :c4 {:zou/dependencies {:tagged :tag2}}})
      {:c1   {:a :b}
       :c2   {:a :b}
       :c3   {:zou/dependencies {:tagged :tag1}}
       :c4   {:zou/dependencies {:tagged :tag2}}
       :tag1 {:zou/dependencies {:c1 :c1 :c2 :c2}}
       :tag2 {:zou/dependencies {:c1' :c1 :c2' :c2}}}))

  (t/is
   (= (#'sut/translate-tags {:c1 {:zou/tags [:tag1]} :tag1 {:a :b}})
      {:c1   {}
       :tag1 {:zou/dependencies {:c1 :c1} :a :b}})))

(t/deftest tags-with-scope-test
  (t/is
   (= (#'sut/translate-tags {:scope/c1 {:zou/tags [:scope/tag1 [:scope/tag2 :c1']] :a :b}})
      {:scope/c1   {:a :b}
       :scope/tag1 {:zou/dependencies {:c1 :scope/c1}}
       :scope/tag2 {:zou/dependencies {:c1' :scope/c1}}})))

(t/deftest build-system-map-test
  (t/testing "dependencies+dependants"
    (let [sys (sut/build-system-map {:c1 {:a :a}
                                     :c2 {:zou/constructor  map->FooComponent
                                          :zou/dependencies {:c1 :c1}
                                          :zou/dependants   {:c7 :c2'}
                                          :b                :b}
                                     :c3 [:a :b]
                                     :c4 false ; should be ignored
                                     :c5 nil   ; should be ignored
                                     :c6 {:zou/disabled true} ; should be ignored
                                     :c7 {}})]
      (t/is (= sys
               (c/map->SystemMap {:c1 {:a :a}
                                  :c2 (map->FooComponent {:b :b})
                                  :c3 [:a :b]
                                  :c7 {}})))
      (t/is (= (c/dependencies (:c2 sys))
               {:c1 :c1}))
      (t/is (= (c/dependencies (:c7 sys))
               {:c2' :c2}))))

  (t/testing "tags"
    (let [sys (sut/build-system-map {:c1 {:zou/tags [:tag1 [:tag2 :c1']]}
                                     :c2 {:zou/dependencies {:tagged :tag1}}
                                     :c3 {:zou/dependencies {:tagged :tag2}}})]
      (t/is (= sys
               (c/map->SystemMap {:c1 {}
                                  :c2 {}
                                  :c3 {}
                                  :tag1 {}
                                  :tag2 {}})))
      (t/is (= (c/dependencies (:c2 sys))
               {:tagged :tag1}))
      (t/is (= (c/dependencies (:c3 sys))
               {:tagged :tag2}))
      (t/is (= (c/dependencies (:tag1 sys))
               {:c1 :c1}))
      (t/is (= (c/dependencies (:tag2 sys))
               {:c1' :c1}))))

  (t/testing "constructor is swappable"
    (let [sys (sut/build-system-map {:zou/constructor map->FooComponent
                                     :c1 :c1'})]
      (t/is (instance? FooComponent sys))
      (t/is (= (:c1 sys) :c1')))

    ;; default
    (let [sys (sut/build-system-map {:c1 :c1'})]
      (t/is (instance? com.stuartsierra.component.SystemMap sys))
      (t/is (= (:c1 sys) :c1')))))

(t/deftest flatten-nested-system-map-test
  (t/is
   (= (#'sut/flatten-nested-system-map {:a {:a {:zou/dependencies {:ab :b :bb :b/b}}
                                            :b {:zou/dependants {:a :b :b/a :b}}}
                                        :b {:a {:zou/tags [:tag1 [:tag2 :ba]]}
                                            :b {:zou/optionals {:ba :a :aa :a/a}}
                                            :c true ; invalid component spec, but doesn't care here
                                            }})
      {:a/a {:zou/dependencies {:ab :a/b :bb :b/b}}
       :a/b {:zou/dependants {:a/a :b :b/a :b}}
       :b/a {:zou/tags [:b/tag1 [:b/tag2 :ba]]}
       :b/b {:zou/optionals {:ba :b/a :aa :a/a}}
       :b/c true})))

(t/deftest subsystem-test
  (let [conf {:c1 {:a                :a
                   :zou/dependencies {:c2' :c2}}
              :c2 {:a                :a
                   :zou/dependencies {:c3' :c3}}
              :c3 {:a :a}}]
    (t/is (= (set (keys (#'sut/extract-subsystem-conf conf [:c3])))
             #{:c3}))
    (t/is (= (set (keys (#'sut/extract-subsystem-conf conf [:c2])))
             #{:c2 :c3}))
    (t/is (= (set (keys (#'sut/extract-subsystem-conf conf [:c1])))
             #{:c1 :c2 :c3}))))

(t/deftest with-component-test
  (let [c (new-stateful-component {})]
    ;; returns started component (c')
    (t/is (= (:started (sut/with-component [c' c] c'))
             true))

    ;; ensure the component has been stopped
    (t/is (= @(:state c) [:started :stopped]))

    (reset! (:state c) [])
    (t/is (thrown? ExceptionInfo
                   (sut/with-component [c' c]
                     (throw (ex-info "error" {})))))
    (t/is (= @(:state c) [:started :stopped]))))

(t/deftest with-system-test
  (t/testing "whole system"
    (let [s (sut/with-system [s {:a {:zou/constructor new-stateful-component}}] s)]
      (t/is (= (get-in s [:a :started])
               true))
      (t/is (= @(get-in s [:a :state]) [:started :stopped]))))

  (t/testing "extracted sub system"
    (let [s (sut/with-system [s {:a {:zou/constructor new-stateful-component}
                                 :b {:zou/constructor new-stateful-component}
                                 :c {:zou/constructor new-stateful-component
                                     :zou/dependencies {:a :a}}}
                              :c] s)]
      (t/is (= (set (keys s)) #{:a :c})))))

(t/deftest try-recovery-test
  (let [sys (c/map->SystemMap {:broken (->BrokenComponent)
                               :dep (new-stateful-component {})})
        sys (c/system-using sys
                            {:broken {:dep :dep}})]
    (try
      (sut/try-recovery
       (c/start sys))
      (catch ExceptionInfo e
        ;; Ensure `dep` is stopped
        (t/is (= (-> sys :dep :state deref set) #{:started :stopped}))))))
