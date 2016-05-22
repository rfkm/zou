(ns zou.component.proto-ext)

;;; CAUTION: Protocol extension does not support CLJS

(defmulti apply-protocol-extension (fn [system component-key proto phase]
                                     ;; In CLJS, `satisfies?` is a
                                     ;; macro and must be passed a
                                     ;; protocol name.
                                     (if (satisfies? proto (get system component-key))
                                       [proto phase]
                                       :default)))

(defmethod apply-protocol-extension :default [system component-key proto phase]
  ;; Do nothing
  system)

(defn protocol-extension-protocols
  "Returns a list of protocols that are defined as a
  protocol-extension."
  []
  (->> (methods apply-protocol-extension)
       keys
       (remove #(= :default %))
       (map first) ;; = (first [proto phase])
       ))

(defn apply-protocol-extensions [phase system]
  {:pre [(#{:instantiated :injected} phase)]}
  (reduce (fn [system' component-key]
            (reduce (fn [system'' protocol]
                      (apply-protocol-extension system'' component-key protocol phase))
                    system'
                    (protocol-extension-protocols)))
          system
          (keys system)))
