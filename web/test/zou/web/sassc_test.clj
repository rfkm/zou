(ns zou.web.sassc-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.test :as t]
            [hawk.core :as hawk]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.task :as task]
            [zou.web.asset.proto :as aproto]
            [zou.web.sassc :as sut]))

(def conf {:src "src.scss"
           :output-to "out.css"
           :style "nested"
           :load-path "lib"
           :sourcemap true
           :precision 3
           :serve-path "/main.css"})

(t/deftest gen-args-test
  (fact "gen-args"
    (#'sut/gen-args conf) => [ "--sourcemap" "--style" "nested" "--precision" "3" "--load-path" "lib" "src.scss" "out.css"]
    (#'sut/gen-args (assoc conf :load-path ["lib" "lib2"]))
    =>
    ["--sourcemap" "--style" "nested" "--precision" "3" "--load-path" "lib" "--load-path" "lib2" "src.scss" "out.css"]))

(t/deftest touch+clean-test
  (fact "touch + clean"
    (let [out "target/sassc/tmp.scss"
          conf {:src "_"
                :output-to out}]
      (sut/touch conf) => anything
      (.exists (io/file out)) => true
      (sut/clean conf) => anything
      (.exists (io/file out)) => false)))

(t/deftest compile-test
  (facts "compile"
    (fact "basic"
      (log/with-test-logger
        (sut/compile {:src "src.scss"
                      :output-to "out.css"}) => anything
        (provided
          (sh/sh "sassc" "src.scss" "out.css") => {:exit 0
                                                   :err "err"
                                                   :out "out"})

        (log/logged? "Successfully compiled src.scss to out.css" :info) => true
        (log/logged? "err" :error) => true
        (log/logged? "out" :info) => true))

    (fact "compile w/ error"
      (log/with-test-logger
        (sut/compile {:src "src.scss"
                      :output-to "out.css"}) => anything
        (provided
          (sh/sh "sassc" "src.scss" "out.css") => {:exit 1})

        (log/logged? "Failed to compile") => true))

    (fact "Show error message if sassc is missing"
      (log/with-test-logger
        (sut/compile {:src "src.scss"
                      :output-to "out.css"
                      :sassc-cmd "nonexistentcmd"}) => anything
        (log/logged? "Did you properly install SassC?") => true))))

(t/deftest watcher-spec-test
  (facts "watcher-spec"
    (fact
      (sut/watcher-spec {:src "src.scss"}) => (just {:paths ["src.scss"]
                                                     :filter (exactly sut/sass-file?)
                                                     :handler fn?})
      (sut/watcher-spec {:src "target/src.scss"}) => (contains {:paths ["target"]})

      (sut/watcher-spec {:src "src.scss"
                         :load-path "lib"}) => (contains {:paths ["src.scss" "lib"]})
      (sut/watcher-spec {:src "src.scss"
                         :load-path ["lib" "lib2"]}) => (contains {:paths ["src.scss" "lib" "lib2"]}))

    (fact
      (let [conf {:src "src.scss"}]
        ((:handler (sut/watcher-spec conf)) {} {}) => anything
        (provided
          (sut/touch conf) => anything
          (sut/compile conf) => anything)))))

(t/deftest component-test
  (facts "SasscCompiler"
    (fact
      (let [c (sut/map->SasscCompiler {:builds [conf]})]
        (c/start c) => c))

    (fact "watcher"
      (let [c (sut/map->SasscCompiler {:builds [conf]
                                       :watch? true})]
        (c/start c) => (assoc c
                              ::sut/watcher ..watcher..)
        (provided
          (hawk/watch! anything) => ..watcher..)

        (c/stop (assoc c ::sut/watcher ..watcher..)) => (assoc c ::sut/watcher nil)
        (provided
          (hawk/stop! ..watcher..) => anything)))

    (fact "initial build"
      (let [c (sut/map->SasscCompiler {:builds [conf]
                                       :initial-build? true})]
        (c/start c) => c
        (provided
          (sut/touch conf) => anything
          (sut/compile conf) => anything)))

    (fact "asset"
      (let [c (sut/map->SasscCompiler {:builds [{:src "src.scss"
                                                 :output-to "out.css"
                                                 :serve-path "/main.css"}
                                                {:src "src2.scss"
                                                 :output-to "out2.css"}]})]
        (aproto/assets c) => [{:name "/main.css"
                               :type :stylesheet
                               :src "out.css"}]))))

(t/deftest task-test
  (fact
    (let [c (sut/map->SasscTask {:builds [conf]})
          ep (task/create-entrypoint (task/task->cmd c) {})]
      (ep "compile") => anything
      (provided
        (sut/touch conf) => anything
        (sut/compile conf) => anything))))
