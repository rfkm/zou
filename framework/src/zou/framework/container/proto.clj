(ns zou.framework.container.proto)

(defprotocol SystemContainer
  (get-system [this system-key])
  (system-keys [this])
  (add-system [this system-key system])
  (remove-system [this system-key])
  (start-system [this system-key])
  (stop-system [this system-key]))
