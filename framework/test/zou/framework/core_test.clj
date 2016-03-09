(ns zou.framework.core-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.bootstrap :as boot]
            [zou.framework.core :as sut]))

(t/deftest boot-shutdown-test
  (fact
    (def core-var nil)
    (prerequisites
     (boot/make-bootstrap-system ..conf..) => ..core..
     (boot/init-bootstrap-system ..core..) => ..started-core..
     (boot/deinit-bootstrap-system ..started-core..) => ..stopped-core..
     (boot/make-bootstrap-system ..conf2..) => ..core2..
     (boot/init-bootstrap-system ..core2..) => ..started-core2..
     (boot/deinit-bootstrap-system ..started-core2..) => ..stopped-core2..)
    core-var => nil
    (sut/boot! #'core-var ..conf..) => ..started-core..
    core-var => ..started-core..
    ;; The old instance bound to the var will be replaced with new one after stopping it
    (sut/boot! #'core-var ..conf2..) => ..started-core2..
    core-var => ..started-core2..

    (sut/shutdown! #'core-var) => nil
    core-var => nil))
