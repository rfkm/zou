(ns leiningen.new.zou
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [leiningen.core.main :as main]
            [leiningen.new.templates :as tpl]))

(def render (tpl/renderer "zou"))
(def +default-profiles+ [:base])
(def +profiles+ [:base :h2 :postgresql :mysql :midje :cljs :enlive :hiccup :sass :scss])

(defn resource [name]
  (io/input-stream (io/resource (str "leiningen/new/zou/" name))))

(defn map->tpl-friendly-list [m]
  (map (fn [[k v]]
         {:key k :val v}) m))

(defn dquote [s]
  (str "\"" s "\""))

(defmulti apply-profile (fn [profile name acc] profile))

(defmethod apply-profile :default [_ _ acc]
  acc)

(defn build-conf [name profiles]
  (let [profiles (filter (into (set profiles) +default-profiles+) +profiles+)]
    (reduce (fn [acc prof]
              (apply-profile prof name acc)) {} profiles)))

(defn gen-from-conf [{:keys [data templates resources]}]
  (apply tpl/->files
         data
         (concat (map (fn [[dest src]] [dest (render src data)]) templates)
                 (map (fn [[dest src]] [dest (resource src)]) resources))))

(defn parse-profiles [profiles]
  (->> profiles
       (filter #(.startsWith % "+"))
       (map #(keyword (subs % 1)))
       set))

(defn zou
  "Generates a new Zou project."
  [name & profiles]
  (let [conf (build-conf name (parse-profiles profiles))]
    (main/info "Generating fresh 'lein new' zou project.")
    (gen-from-conf conf)))


;;; impls

(defmethod apply-profile :base [_ name acc]
  (let [raw-name name
        name (tpl/project-name raw-name)
        sanitized-name (tpl/sanitize name)
        ns (tpl/sanitize-ns raw-name)
        dirs (tpl/name-to-path raw-name)]
    (-> acc
        (update-in [:data] merge
                   {:raw-name       raw-name
                    :name           name
                    :sanitized-name sanitized-name
                    :dirs           dirs
                    :namespace      ns
                    :uberjar-name   (str name ".jar")})
        (update-in [:templates] merge
                   {"project.clj"                        "project.clj"
                    "resources/zou/config/config.edn"    "config.edn"
                    "resources/zou/config/bootstrap.edn" "bootstrap.edn"
                    "src/zou/ext/repl/{{dirs}}.clj"      "ext_repl.clj"
                    "src/{{dirs}}/routes.clj"            "routes.clj"
                    "src/{{dirs}}/handler/main.clj"      "handler_main.clj"
                    "src/{{dirs}}/view/main.clj"         "view_main.clj"
                    "src/{{dirs}}/view_helper.clj"       "view_helper.clj"})
        (update-in [:resources] merge
                   {".gitignore" ".gitignore"}))))

(defmethod apply-profile :h2 [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:h2? true
                  :db? true
                  :dbspecs (map->tpl-friendly-list
                            {:adapter (dquote "h2")
                             :url     (dquote (str "jdbc:h2:./" (get-in acc [:data :sanitized-name])))})})))

(defmethod apply-profile :postgresql [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:h2? false
                  :postgresql? true
                  :db? true
                  :dbspecs (map->tpl-friendly-list
                            {:adapter       (dquote "postgresql")
                             :database-name (dquote (get-in acc [:data :sanitized-name]))})})))

(defmethod apply-profile :mysql [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:h2? false
                  :postgresql? false
                  :mysql? true
                  :db? true
                  :dbspecs (map->tpl-friendly-list
                            {:adapter       (dquote"mysql")
                             :database-name (dquote (get-in acc [:data :sanitized-name]))
                             :username      (dquote "root")
                             :password "nil"})})))

(defmethod apply-profile :midje [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:midje? true})))

(defmethod apply-profile :cljs [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:cljs? true})
      (update-in [:templates] merge
                 {"resources/zou/config/cljsbuild.edn" "cljsbuild.edn"
                  "src-cljs/{{dirs}}/core.cljs"        "core.cljs"})))

(defmethod apply-profile :enlive [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:enlive? true
                  :template? true})
      (update-in [:templates] merge
                 {"resources/templates/layout.html" "layout.html"})))

(defmethod apply-profile :hiccup [_ name acc]
  (-> acc
      (update-in [:data] merge
                 {:hiccup? true
                  :template? true})))

(defn- check-sassc []
  (when-not (try (sh/sh "sassc" "-h")
                 (catch Throwable _))
    (main/warn "[WARN] You don't seem to install SassC. To use Sass component, you need to install it before running your app.")))

(defmethod apply-profile :sass [_ name acc]
  (check-sassc)
  (-> acc
      (update-in [:data] merge
                 {:sassc? true
                  :sass? true})
      (update-in [:templates] merge
                 {"src-sass/main.sass" "main.sass"
                  "src-sass/_sub.sass" "_sub.sass"})))

(defmethod apply-profile :scss [_ name acc]
  (check-sassc)
  (-> acc
      (update-in [:data] merge
                 {:sassc? true
                  :scss? true})
      (update-in [:templates] merge
                 {"src-scss/main.scss" "main.scss"
                  "src-scss/_sub.scss" "_sub.scss"})))

(comment
  (do
    (defn delete-recursively [fname]
      (let [del (fn del [f]
                  (when (.isDirectory f)
                    (doseq [f' (.listFiles f)]
                      (del f')))
                  (io/delete-file f))]
        (del (io/file fname))))

    (let [app "foo/my-app"
          name (tpl/project-name app)
          profiles ["+h2" "+midje" "+cljs" "+enlive" "+hiccup" "+scss"]]
      (when (.exists (io/file name))
        (delete-recursively name))
      (apply zou app profiles))))
