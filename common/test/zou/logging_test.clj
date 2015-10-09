(ns zou.logging-test
  (:require [clojure.test :as t]
            [io.aviso.logging :as pretty]
            [midje.sweet :refer :all]
            [unilog.config :as unilog]
            [zou.logging :as sut]))

(t/deftest logging-test
  (facts "staet-logging!"
    (sut/start-logging! ..conf..) => anything
    (provided
      (pretty/install-pretty-logging) => anything
      (pretty/install-uncaught-exception-handler) => anything
      (unilog/start-logging! ..conf..) => anything)))

(t/deftest test-logger-test
  (fact
    (sut/with-test-logger
      (sut/info "foo")
      (sut/info "bar")
      [(sut/logged? "foo")
       (sut/logged? "foo" :info)
       (sut/logged? "foo" :debug)
       (sut/logged? #"^b")])
    =>
    [true true false true])

  (fact
    (sut/with-test-logger
      (sut/not-logged?))
    =>
    true)

  (fact
    (sut/with-test-logger
      (sut/not-logged?))
    =>
    true

    (sut/with-test-logger
      (sut/info "foo")
      (sut/not-logged?))
    =>
    false

    (sut/with-test-logger
      (sut/info "foo")
      [(sut/not-logged? "foo")
       (sut/not-logged? "baz")])
    =>
    [false true]))
