(ns zou.db.tx-test
  (:require [zou.db.tx :as sut]
            [clojure.test :as t]
            [cats.core :as m]))

(def tx1 (reify sut/Tx
           (-id [this] :tx1)
           (-atomic [this ctx f]
             (f (update ctx :cn (fnil inc 0))))))

(def tx2 (reify sut/Tx
           (-id [this] :tx2)
           (-atomic [this ctx f]
             (f (update ctx :cn (fnil inc 0))))))


(t/deftest local-context-test
  (t/is (= @(m/mlet [a (sut/task tx1 identity)
                     b (sut/task tx1 identity)
                     c (sut/task tx2 identity)
                     d (sut/task tx1 identity)
                     e (sut/task tx2 identity)]
              (m/return [a b c d e]))
           [{:cn 1}
            {:cn 2}
            {:cn 1}
            {:cn 3}
            {:cn 2}])))

(t/deftest fmap-test
  (t/is (= @(m/fmap inc (sut/task tx1 (fn [_] 1)))
           2)))
