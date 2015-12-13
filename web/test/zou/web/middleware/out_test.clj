(ns zou.web.middleware.out-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.util.namespace :as un]
            [zou.web.middleware.out :as sut]))

(def ^:private ^:dynamic *msg* {:id 100})

(t/deftest wrap-nrepl-out-bridge-test
  (fact
    (against-background
      (un/safe-resolve-var 'clojure.tools.nrepl.middleware.interruptible-eval/*msg*)
      =>
      #'*msg*)
    (let [bakout *out*
          bakerr *err*
          strout (java.io.StringWriter.)
          strerr (java.io.StringWriter.)
          h (fn [req]
              (print (str "out" (:id *msg*)))
              (binding [*out* *err*]
                (print (str "err" (:id *msg*)))))]
      (binding [*out* strout
                *err* strerr]
        (let [h' (sut/wrap-nrepl-out-bridge h)]
          (binding [*out* bakout
                    *err* strerr]
            (h' {}))))
      (str strout) => "out100"
      (str strerr) => "err100"))

  (fact "non-nrepl circumstances"
    (against-background
      (un/safe-resolve-var 'clojure.tools.nrepl.middleware.interruptible-eval/*msg*)
      =>
      nil)
    (let [h (fn [])]
      (sut/wrap-nrepl-out-bridge h) => (exactly h))))
