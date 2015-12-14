(ns zou.util.repl-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.util.repl :as sut]))

(t/deftest out-bridge-test
  (fact "out"
    (try
      (with-out-str
        (sut/bridge-out!)
        (.print System/out "foo")) => "foo\n"
      (finally
        (sut/restore-out!))))

  (fact "err"
    (try
      (with-out-str
        (binding [*err* *out*] (sut/bridge-out!))
        (.print System/err "foo")) => "foo\n"
      (finally
        (sut/restore-out!)))))
