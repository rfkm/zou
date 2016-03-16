(ns zou.web.routing.context
  (:require [schema.core :as s]
            [zou.web.context :as ctx]
            [zou.web.routing.proto :as proto]))

;;; A router component that takes the servelet context into account
(s/defrecord ContextAwareRouter [parent-router :- (s/protocol proto/Router)
                                 request-context :- (s/protocol ctx/RequestAccessor)]
  proto/Router
  (match [this req]
    (proto/match parent-router req))

  (unmatch [this route-id params]
    (when-let [m (proto/unmatch parent-router route-id params)]
      (str
       (:context (ctx/current-request request-context))
       m))))
