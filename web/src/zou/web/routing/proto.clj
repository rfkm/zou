(ns zou.web.routing.proto)

(defprotocol Router
  (match [this req]
    "Returns a map that includes matched info.

     E.g.
     {:route-params {:post-id \"123\"}
      :route-id :show-comments
      :handler #'acme.handler.post/show-comments}")
  (unmatch [this route-id params]
    "Generate a path string for the given route-id.
     Some routing library may not be able to implement this."))
