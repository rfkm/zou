(ns zou.web.handler.args-mapper-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.web.handler.args-mapper :as sut]))

(background
 (before :facts
         (do
           (defmethod sut/process-param "test:" [m]
             (let [k (keyword (subs (name (:sym m)) 5))]
               (assoc m :fn #(get-in % [:test k]))))

           (defmethod sut/process-param "alias:" [m]
             (let [k (keyword (subs (name (:sym m)) 6))]
               (assoc m
                      :alias (symbol (name k))
                      :fn #(get-in % [:test k]))))
           (defmethod sut/process-param "?foo" [m]
             (if (= (:sym m) '?foo)
               (assoc m :fn (constantly :foo))
               (sut/skip m)))

           (defmethod sut/process-param "?" [m]
             (assoc m :fn (constantly :?)))))
 (after :facts
        (do
          (remove-method sut/process-param "test:")
          (remove-method sut/process-param "alias:")
          (remove-method sut/process-param "?foo")
          (remove-method sut/process-param "?"))))


(t/deftest dispatch-param-processor-test
  (fact
    (against-background
      (methods sut/process-param) => {"$" anything
                                      "&" anything
                                      "&>" anything
                                      nil anything})
    (#'sut/dispatch-param-processor {:sym '$a}) => "$"
    (#'sut/dispatch-param-processor {:sym '&a}) => "&"
    (#'sut/dispatch-param-processor {:sym '&>a}) => "&>"
    (#'sut/dispatch-param-processor {:sym 'a}) => nil))

(t/deftest grouping-test
  (fact
    (#'sut/group-params '[a b c :as d f :- g]) => '[a b [c :as d] [f :- g]]))

(t/deftest process-param-test
  (fact
    ((:fn (sut/process-param {:sym 'test:a})) {:test {:a ..ans..}}) => ..ans..))

(t/deftest skip-test
  (fact
    ((:fn (sut/process-param {:sym '?foo})) {}) => :foo
    ((:fn (sut/process-param {:sym '?})) {}) => :?
    ((:fn (sut/process-param {:sym '?foobar})) {}) => :?))

(t/deftest gen-destructuring-spec-test
  (fact
    (let [ret (sut/gen-destructuring-spec '[test:a test:b :as b' test:c :as c test:d :<< as-long alias:a])]
      ret => (just {:params '[test:a b' c test:d a] :fn fn?})
      ((:fn ret) {:test {:a ..a..
                         :b ..b..
                         :c ..c..
                         :d "100"}})
      =>
      [..a.. ..b.. ..c.. 100 ..a..]))

  (fact
    (let [ret (sut/gen-destructuring-spec [])]
      ret => (just {:params [] :fn fn?})
      ((:fn ret) ..a..)
      =>
      [])))

(t/deftest nested-infix-op-test
  (fact
    (let [m (sut/gen-destructuring-spec '[test:a :<< as-long :as a'])]
      m => (just {:params '[a']
                  :fn fn?})
      ((:fn m) {:test {:a "100"}}) => [100])))

(t/deftest exception-test
  (fact
    (sut/gen-destructuring-spec '[:as a]) => (throws "Invalid format of parameters")
    (sut/gen-destructuring-spec '[a :as]) => (throws "Invalid format of parameters")
    (sut/gen-destructuring-spec '[a :<<]) => (throws "Invalid format of parameters")
    (sut/gen-destructuring-spec '[a :foo a]) => (throws "Unknown infix operator: :foo")
    (sut/gen-destructuring-spec '[a :<< foo]) => (throws "Unknown coercer: foo")))
