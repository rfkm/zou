(ns zou.web.asset-test
  (:require [bidi.bidi :as bidi]
            [clojure.java.io :as io]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.web.asset :as sut]
            [zou.web.asset.proto :as aproto]
            [zou.web.response :as res]
            [zou.web.view.proto :as vproto]))

(defrecord TestProvider [data]
  aproto/AssetsProvider
  (assets [this] data))

(defn gen-asset-manager [data]
  (sut/map->AssetManager {:asset-providers {:test (->TestProvider data)}}))

(t/deftest asset-test
  (fact
    (let [am (gen-asset-manager [{:id :foo
                                  :name "/foo.js"
                                  :type :javascript
                                  :src "project.clj"}])]
      (vproto/view-model am) => {:javascripts [:foo]
                                 :stylesheets []}

      (bidi/routes am) => (just ["" (just [(just ["/foo.js" (just (bidi/tag fn? :foo))])])])
      ((-> (bidi/routes am) second first second :matched) {}) => {:body (io/file "project.clj")}))

  (fact
    (let [src (io/resource "project.clj")
          am (gen-asset-manager [{:id :foo
                                  :name "/foo.js"
                                  :type :javascript
                                  :src src}])]
      (vproto/view-model am) => {:javascripts [:foo]
                                 :stylesheets []}

      (bidi/routes am) => (just ["" (just [(just ["/foo.js" (just (bidi/tag fn? :foo))])])])
      ((-> (bidi/routes am) second first second :matched) {}) => ..res..
      (provided
        (res/url-response src) => ..res..))))
