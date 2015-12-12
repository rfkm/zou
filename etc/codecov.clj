(require '[clojure.java.io :as io]
         '[cheshire.core :as json])

(def covfile "target/coverage/codecov.json")
(def outfile "target/coverage/codecov_fixed.json")

(defn fix-path [path]
  (str (.relativize (.toURI (io/file ".")) (.toURI (io/resource path)))))

(defn fix-paths [m]
  (into {} (map (fn [[k v]] [(fix-path k) v]) m)))

(defn fix-covfile [in out]
  (-> (slurp covfile)
      json/parse-string
      (update-in ["coverage"] fix-paths)
      json/generate-string
      (->> (spit outfile))))

(fix-covfile covfile outfile)

;; Local Variables:
;; cider-lein-parameters: "with-profile +coverage repl :headless"
;; End:
