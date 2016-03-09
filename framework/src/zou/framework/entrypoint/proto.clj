(ns zou.framework.entrypoint.proto)

(defprotocol EntryPoint
  (run [this args]))

(defn entrypoint? [x]
  (satisfies? EntryPoint x))
