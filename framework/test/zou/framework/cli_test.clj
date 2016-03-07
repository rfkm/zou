(ns zou.framework.cli-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.framework.cli :as sut]
            [clojure.java.io :as io]))


(t/deftest extract-config-file-test
  (fact
    (sut/extract-config-file ["-c" "project.clj"]) => (io/file "project.clj"))

  (fact
    (sut/extract-config-file ["-c" "non-existent-file"]) => (throws IllegalArgumentException #"Failed to load config file")))
