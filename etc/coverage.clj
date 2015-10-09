(require '[clojure.java.io :as io]
         '[cheshire.core :as json])

(def covfile "target/coverage/coveralls.json")
(def outfile "target/coverage/coveralls.edn")

(defn fix-path [path]
  (str (.relativize (.toURI (io/file ".")) (.toURI (io/resource path)))))

(defn fix-source [m]
  (update-in m [:name] fix-path))

(-> (slurp covfile)
    (json/parse-string true)
    (update-in [:source_files] (partial map fix-source))
    prn-str
    (->> (spit outfile)))
