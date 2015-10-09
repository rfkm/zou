(require '[clojure.java.io :as io]
         '[cheshire.core :as json]
         '[environ.core :refer [env]]
         '[plumbing.core :as p]
         '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str])

(def covfile "target/coverage/coveralls.edn")

(def outfile "coveralls.json")

(def subdirs (rest *command-line-args*))

(defn read-covfile [module]
  (read-string (slurp (str module "/" covfile))))

(defn fix-path [prefix path]
  (str prefix "/" path))

(defn fix-source [module m]
  (update-in m [:name] (partial fix-path module)))

(defn merge-covs [& ms]
  (-> (apply merge ms)
      (assoc :source_files
             (apply concat (map :source_files ms)))))

(defn g [& c]
  (try
    (let [ret (apply sh c)]
      (when (zero? (:exit ret)) (when-let [h (:out ret)] (str/trim h))))
    (catch Throwable _)))

(defn git-conf []
  {:head    {:id              (env :git-id (g "git" "log" "-1" "--pretty=format:%H"))
             :author_name     (env :git-author-name (g "git" "log" "-1" "--pretty=format:%aN"))
             :author_email    (env :git-author-email (g "git" "log" "-1" "--pretty=format:%ae"))
             :committer_name  (env :git-committer-name (g "git" "log" "-1" "--pretty=format:%cN"))
             :committer_email (env :git-committer-email (g "git" "log" "-1" "--pretty=format:%ce"))
             :message         (env :git-message (g "git" "log" "-1" "--pretty=format:%s"))}
   :branch  (env :git-branch (g "git" "rev-parse" "--abbrev-ref" "HEAD"))
   :remotes (distinct (map (fn [r] (zipmap [:name :url] (str/split r #"\s")))
                           (str/split-lines (g "git" "remote" "-v"))))})

(defn gen-config []
  (-> {:service_name         "circleci"
       :service_number       (env :circle-build-num)
       :service_pull_request (first (re-seq #"\d+$" (or (env :ci-pull-request) "")))
       :parallel             (> (Integer/parseInt (env :circle-node-total "1")) 1)
       :service_job_number   (env :circle-node-index)
       :environment          {:circle-build-num (env :circle-build-num)
                              :branch           (env :circle-branch)
                              :commit-sha       (env :circle-sha1)}
       :git                  (git-conf)}
      (p/assoc-when :repo_token (env :coveralls-repo-token))))

(defn cov [module]
  (let [path (str module "/" covfile)
        m    (read-string (slurp path))]
    (update-in m [:source_files] (partial map (partial fix-source module)))))

(->> subdirs
     (map cov)
     (apply merge-covs)
     (#(merge % (gen-config)))
     json/generate-string
     (spit outfile))
