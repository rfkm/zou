(ns zou.web.finder.metadata-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.web.finder.metadata :as sut]
            [zou.web.finder.proto :as proto]))

(defn ^{::tag :foo} foo [])

(t/deftest component-test
  (fact
    (c/with-component [r (sut/->MetadataBasedFinder true ::tag "zou.web.finder")]
      (proto/find r identity) => identity
      (proto/find r :foo) => (exactly foo)
      (proto/find r :invalid) => nil))

  (fact "w/ non-existent ns-prefix"
    (c/with-component [r (sut/->MetadataBasedFinder true ::tag "foo")]
      (proto/find r identity) => identity
      (proto/find r :foo) => nil))

  (fact "Shows warning message when conflicted tags were found"
    (log/with-test-logger
      (c/with-component [r (sut/->MetadataBasedFinder true ::tag "zou.web.finder")]
        (defn ^{::tag :foo} bar [])
        (proto/find r :foo) => (exactly foo)
        (log/logged? #"Conflicted tags: :foo" :warn) => true
        (ns-unmap *ns* 'bar))))

  (fact "Finds handlers only once if dynamic? is false"
    (c/with-component [r (sut/->MetadataBasedFinder false ::tag "zou.web.finder")]
      (proto/find r :foo) => (exactly foo)
      (defn ^{::tag :bar} bar [])
      (proto/find r :bar) => nil
      (ns-unmap *ns* 'bar))))
