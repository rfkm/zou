(ns zou.framework.entrypoint.proto)

(defprotocol EntryPoint
  (run [this args]))
