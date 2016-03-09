(ns zou.framework.container.proto)

(defprotocol ComponentContainer
  (get-component [this component-key])
  (as-system [this])
  (component-keys [this])
  (add-component [this component-key component])
  (remove-component [this component-key])
  (start-system [this])
  (stop-system [this])
  (narrow [this ks]))
