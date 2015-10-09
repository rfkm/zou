(ns zou.framework.ext-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.framework.ext :as sut]))

(t/deftest ext-test
  (fact "extension-namespaces"
    (sut/extension-namespaces) => (contains ['zou.ext.test-extension]))

  (fact "extension-initializer"
    (let [ext-ns 'zou.ext.test-extension]
      (remove-ns ext-ns)
      (dosync (alter @#'clojure.core/*loaded-libs* disj ext-ns))
      (let [ret (sut/extension-initializer ext-ns)]
        ret       => var?
        (str ret) => "#'zou.ext.test-extension/init")))

  (fact "initialize-extension"
    (defn a [] :a)
    (sut/initialize-extension 'ext-ns) => :a
    (provided
      (sut/extension-initializer 'ext-ns) => #'a))

  (fact "initialize-extensions"
    (sut/initialize-extensions) => nil
    (provided
      (sut/extension-namespaces) => ['a 'b]
      (sut/initialize-extension 'a) => nil
      (sut/initialize-extension 'b) => nil))

  (fact "extension-deinitializer"
    (let [ext-ns 'zou.ext.test-extension]
      (remove-ns ext-ns)
      (dosync (alter @#'clojure.core/*loaded-libs* disj ext-ns))
      (let [ret (sut/extension-deinitializer ext-ns)]
        ret       => var?
        (str ret) => "#'zou.ext.test-extension/deinit")))

  (fact "deinitialize-extension"
    (defn a [] :a)
    (sut/deinitialize-extension 'ext-ns) => :a
    (provided
      (sut/extension-deinitializer 'ext-ns) => #'a))

  (fact "deinitialize-extensions"
    (sut/deinitialize-extensions) => nil
    (provided
      (sut/extension-namespaces) => ['a 'b]
      (sut/deinitialize-extension 'a) => nil
      (sut/deinitialize-extension 'b) => nil)))
