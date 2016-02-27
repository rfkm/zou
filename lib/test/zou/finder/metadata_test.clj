(ns zou.finder.metadata-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.util.namespace :as un]
            [zou.finder.metadata :as sut]
            [zou.finder.proto :as proto]))

(defn ^{::tag :foo} foo [])

(t/deftest component-test
  (fact
      (c/with-component [r (sut/map->MetadataBasedFinder {:dynamic? true :var-tag ::tag})]
        (proto/find r identity) => identity
        (proto/find r :foo) => (exactly foo)
        (proto/find r :invalid) => nil))

  (fact "w/ ns-tag"
    (c/with-component [r (sut/map->MetadataBasedFinder {:dynamic? true :var-tag :my/handler-var :ns-tag :my/handler-ns})]
      (proto/find r identity) => identity
      (un/with-temp-ns [ns '((defn ^{:my/handler-var :foo} foo []))
                        ns2 '((defn ^{:my/handler-var :bar} bar [])
                              (alter-meta! *ns* assoc :my/handler-ns true))]
        (proto/find r :foo) => nil
        (proto/find r :bar) => (exactly @(ns-resolve ns2 'bar)))))

  (fact "Shows warning message when conflicted tags were found"
    (log/with-test-logger
      (c/with-component [r (sut/map->MetadataBasedFinder {:dynamic? true :var-tag ::tag})]
        (defn ^{::tag :foo} bar [])
        (proto/find r :foo) => (exactly foo)
        (log/logged? #"Conflicted tags: :foo" :warn) => true
        (ns-unmap *ns* 'bar))))

  (fact "Finds handlers only once if dynamic? is false"
    (c/with-component [r (sut/map->MetadataBasedFinder {:dynamic? false :var-tag ::tag})]
      (proto/find r :foo) => (exactly foo)
      (defn ^{::tag :bar} bar [])
      (proto/find r :bar) => nil
      (ns-unmap *ns* 'bar))))
