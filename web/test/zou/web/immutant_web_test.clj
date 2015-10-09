(ns zou.web.immutant-web-test
  (:require [clj-http.client :as http]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.web.handler.proto :as h]
            [zou.web.immutant-web :as sut]))

(def base-url "http://localhost:3021")

(def conf {:port 3021})

(def conf2 (assoc conf :path "/foo"))

(defn gen-url [path]
  (str base-url path))

(defn http-get [path]
  (http/get (gen-url path)))

(defrecord Endpoint [h]
  h/RingHandler
  (handler [this]
    h))

(defn new-system [conf h h2]
  (c/system-map
   :ep (->Endpoint h)
   :ep2 (->Endpoint h2)
   :webserver (c/using
               (sut/map->WebServer conf)
               {:handler :ep})
   :webserver2 (c/using
                (sut/map->WebServer conf2)
                {:handler :ep2})))

(t/deftest immutant-web-test
  (facts "webserver"
    (fact "default handler"
      (c/with-component [_ (sut/map->WebServer conf)]
        (http-get "/") => (contains {:status 200
                                     :body   "Running"})))

    (fact "custom handler & multiple endpoint"
      (c/with-component [s (new-system conf
                                       (constantly {:status 200
                                                    :body "foo"})
                                       (constantly {:status 200
                                                    :body "foo2"}))]
        (get-in s [:webserver :handler]) => #(instance? Endpoint %)
        (http-get "/") => (contains {:status 200
                                     :body   "foo"})
        (http-get "/foo") => (contains {:status 200
                                        :body   "foo2"})))))
