(defproject ca.orchard-labs/morph "1.1.0"
  :description "A small collection of useful transformations"
  :url "http://github.com/orchard-labs/morph"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [camel-snake-kebab "0.4.0"]
                 [com.rpl/specter "1.1.0"]
                 [com.taoensso/truss "1.5.0"]
                 [clj-time "0.14.2"]]

  :repl-options {:init-ns user
                 :color   false}

  :eftest {:multithread?    false
           :capture-output? true
           :report          eftest.report.progress/report}

  :profiles {:dev {:source-paths   ["src"]
                   :test-paths     ["test"]

                   :dependencies [[circleci/bond "0.3.1"]
                                  [eftest "0.5.0"]
                                  [org.clojure/test.check "0.10.0-alpha2"]
                                  [com.gfredericks/test.chuck "0.2.8"]
                                  [viebel/codox-klipse-theme "0.0.5"]]

                   :plugins [[test2junit "1.3.3"]
                             [lein-eftest "0.5.0"]
                             ;; for generating API docs
                             [lein-codox "0.10.3"]]

                   :codox {:metadata {:doc/format :markdown}}

                   :test2junit-output-dir "target/test2junit"
                   :test2junit-run-ant    true

                   :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}}

  :repositories [["snapshots" {:url "https://clojars.org/repo"
                               :username "j0ni"
                               :password :env}]
                 ["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]

  :deploy-repositories [["releases" :clojars]])
