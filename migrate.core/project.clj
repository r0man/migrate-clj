(defproject migrate/migrate.core "0.0.9-SNAPSHOT"
  :description "Core migration library for use in applications"
  :url "http://github.com/r0man/migrate-clj"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[environ "0.2.1"]
                 [clj-time "0.4.3"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.clojure/tools.logging "0.2.3"]]
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[org.clojure/java.jdbc "0.2.1"]
                                  [org.slf4j/slf4j-log4j12 "1.6.4"]
                                  [postgresql/postgresql "9.1-901.jdbc4"]]}}
  :eval-in :leiningen
  ;; :plugins [[environ/environ.lein "0.2.1"]]
  ;; :hooks [environ.leiningen.hooks]
  ;; :migrate [migrate.test.examples]
  )
