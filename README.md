# Zou

[![Circle CI](https://circleci.com/gh/rfkm/zou.svg?style=svg&circle-token=1b92c8b8004d0c119f6424c31fdeaf36756e3e90)](https://circleci.com/gh/rfkm/zou)
[![codecov.io](https://codecov.io/github/rfkm/zou/coverage.svg?branch=master&token=C99JZMX9ml)](https://codecov.io/github/rfkm/zou?branch=master)

**Zou** is a component-based framework for Clojure applications.

# Setup

Zou is not hosted on standard repositories like Clojars or Maven Central but our own repository.
So you need to add our repository to your `project.clj`:

```clj
(defproject your/app "0.1.0-SNAPSHOT"
  ...
  :repositories [["zou-repo"
                  {:url "https://s3.amazonaws.com/zou-repo"}]]
  :dependencies [...
                 [zou "0.1.0-alpha5-SNAPSHOT"]
                 ...]
  ...)
```

## License

Copyright © 2015-2016 Ryo Fukumuro

Distributed under the Eclipse Public License, the same as Clojure.
