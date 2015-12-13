(ns zou.web.middleware.aggregator-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.logging :as log]
            [zou.web.context :as ctx]
            [zou.web.middleware.aggregator :as sut]
            [zou.web.middleware.proto :as proto]))

(defn dummy-middleware [k h]
  (fn [req] (-> req
                (update-in [:req] conj k)
                h
                (update-in [:res] conj k))))

(defn dummy-handler [req]
  (assoc req :res []))

(def base-conf {:middlewares {:a (partial dummy-middleware :a)
                              :b (partial dummy-middleware :b)
                              :c (partial dummy-middleware :c)
                              :d "invalid"}
                :dependency-map {:a {:b :before}
                                 :c {:a :after}}
                :configurator (fn [ms this]
                                (log/debug "configurator is called")
                                ms)})

(t/deftest middleware-aggregator-test
  (facts "MiddlewareAggregator"
    (fact "basic"
      (log/with-test-logger
        (let [ma (sut/map->MiddlewareAggregator base-conf)]
          ((proto/wrap ma dummy-handler) {:req []}) => {:req [:a :b :c]
                                                        :res [:c :b :a]}

          (map :msg @log/*test-logger-entries*) => ["configurator is called"
                                                    "Applying middleware: :c"
                                                    "Applying middleware: :b"
                                                    "Applying middleware: :a"])))

    (fact "w/ context updater"
      (let [m (fn [k h]
                (fn [_]
                  ;; Get a request map from context
                  (-> (#'ctx/request)
                      (update-in [:req] conj k)
                      h
                      (update-in [:res] conj k))))
            ma (sut/map->MiddlewareAggregator (assoc base-conf
                                                     :middlewares {:a (partial m :a)
                                                                   :b (partial m :b)
                                                                   :c (partial m :c)}
                                                     :inject-context-updater? true))]
        ((ctx/wrap-initial-context (proto/wrap ma dummy-handler)) {:req []}) => {:req [:a :b :c]
                                                                                 :res [:c :b :a]}))))
