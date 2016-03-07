(ns zou.framework.container.proto
  (:refer-clojure :exclude [get keys remove]))

(defprotocol SystemContainer
  (get-system [this system-key])
  (system-keys [this])
  (add-system [this system-key system])
  (remove-system [this system-key])
  (start-system [this system-key])
  (stop-system [this system-key]))
