(ns zou.component-test
  (:require [com.stuartsierra.component :as c]
            [zou.component :as sut :include-macros true]
            [zou.component.proto-ext :as pe]
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

(defprotocol MyProtocolExtension
  (inject-constant [this v]))

(defmethod pe/apply-protocol-extension [MyProtocolExtension :instantiated] [system component-key _ _]
  (update-in system [component-key] inject-constant :foo))

(defrecord ProtocolExtensionComponent []
  c/Lifecycle
  (start [this] this)
  (stop [this] this)
  MyProtocolExtension
  (inject-constant [this v]
    (assoc this :constant v)))

(defn new-stateful-component [_]
  (->StatefulComponent (atom [])))

(t/deftest build-system-map-test
  (t/testing "dependencies+dependants"
    (let [sys (sut/build-system-map {:c1 {:a :a}
                                     :c2 {:zou/constructor  map->FooComponent
                                          :zou/dependencies {:c1 :c1}
                                          :zou/dependants   {:c7 :c2'}
                                          :zou/optionals    {:c1' :c1
                                                             :dummy :dummy}
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
               {:c1 :c1
                :c1' :c1}))
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
      (t/is (= (:c1 sys) :c1'))))

  #?(:clj
     (t/testing "protocol extension"
       (let [sys (sut/build-system-map {:pec {:zou/constructor map->ProtocolExtensionComponent
                                              :a :a}})]
         (t/is (= (get-in sys [:pec :constant]) :foo))))))

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
                                     :zou/dependencies {:a :a}}
                                 :d {:zou/constructor new-stateful-component
                                     :zou/dependencies {:c :c}}}
                              :d] s)]
      (t/is (= (set (keys s)) #{:a :c :d})))))

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
