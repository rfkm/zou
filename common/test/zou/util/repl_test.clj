(ns zou.util.repl-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.util.repl :as sut]))

(t/deftest out-bridge-test
  (fact "out"
    (let [r (repeat 10000 "あ")
          expect (str/join r)]
      (try
        (with-out-str
          (sut/bridge-out!)
          (doseq [c r]
            (.print System/out c))) => expect
        (finally
          (sut/restore-out!)))))

  (fact "err"
    (let [r (repeat 10000 "あ")
          expect (str/join r)]
      (try
        (with-out-str
          (binding [*err* *out*] (sut/bridge-out!))
          (doseq [c r]
            (.print System/err c))) => expect
        (finally
          (sut/restore-out!))))))
