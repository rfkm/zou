(ns zou.web.handler.proto)

(defprotocol RingHandler
  (handler [this]))

(extend-protocol RingHandler
  clojure.lang.AFn
  (handler [f]
    f))
