(ns zou.framework.container-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.container :as sut]
            [zou.framework.container.impl :as impl]))

(defrecord Foo []
  c/Lifecycle
  (start [this]
    (assoc this :started true))
  (stop [this]
    (assoc this :stopped true)))

(t/deftest container-test
  (fact "container"
    (let [new-container #(c/start (impl/new-default-container {:spec {:s {:c {:zou/constructor map->Foo}}}}))]

      ;; lifecycle test
      (fact "start"
        (let [container (new-container)]
          (get-in (sut/system container) [:s/c :started]) => nil
          (sut/start-system! container) => container
          (get-in (sut/system container) [:s/c :started]) => true))

      (fact "stop"
        (let [container (new-container)]
          (get-in (sut/system container) [:s/c :stopped]) => nil
          (sut/stop-system! container) => container
          (get-in (sut/system container) [:s/c :stopped]) => true))

      (fact "stop container"
        (let [container (new-container)]
          (sut/start-system! container) => container
          (c/stop container) => anything
          ;; should stop user systems too
          (get-in (sut/system container) [:s/c :stopped]) => true)))))
