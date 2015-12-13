(ns zou.web.middleware.reload-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.middleware.reload :as dep]
            [zou.fixtures.enlive-ns :as ens]
            [zou.logging :as log]
            [zou.web.middleware.reload :as sut]))

(facts "wrap-reload-ns"
  (log/with-test-logger
    (let [h (sut/wrap-reload-ns identity {:ns-finder-fn (constantly [*ns*])})
          ns-name (ns-name *ns*)]
      (h ..req..) => ..req..
      (provided
        (require ns-name :reload) => anything)
      (log/logged? "Reloading: zou.web.middleware.reload-test") => true)))

(t/deftest wrap-reload-enlive-test
  (facts "wrap-reload-enlive"
    (let [h (sut/wrap-reload-enlive identity)]
      (dotimes [n 3]
        (let [init (ens/render)
              touch (fn [] (let [f (io/file ens/tmpl)]
                             (.setLastModified f (+ (.lastModified f) (* 1000 n)))))]
          ;; Update template
          (spit ens/tmpl (str "<p>" (rand) "</p>"))
          (touch)

          ;; Doesn't reflect the update yet
          init => (ens/render)

          ;; Run the handler
          (h {})

          ;; Should reflect the update
          init =not=> (ens/render))))))

(t/deftest wrap-reload-test
  (fact "wrap-reload"
    (sut/wrap-reload ..handler.. ..opts..) => ..ret..
    (provided
      (dep/wrap-reload ..handler.. ..opts..) => ..handler'..
      (sut/wrap-reload-tagged-ns ..handler'..) => ..ret..)))
