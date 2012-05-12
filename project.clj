(defproject migrate/migrate "0.0.9-SNAPSHOT"
  :description "Rails-like Database Migrations for Clojure"
  :min-lein-version "2.0.0"
  :url "http://github.com/r0man/migrate-clj"
  :autodoc {:name "Rails-like Database Migrations for Clojure",
            :author "Roman Scherer",
            :web-src-dir "http://github.com/r0man/migrate-clj/blob/",
            :web-home "http://github.com/r0man/migrate-clj",
            :copyright "Copyright (c) 2010 Roman Scherer"}
  :dependencies [[environ "0.2.1"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]]
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[org.clojure/java.jdbc "0.2.1"]
                                  [org.slf4j/slf4j-log4j12 "1.6.4"]
                                  [postgresql/postgresql "9.1-901.jdbc4"]]}}
  :eval-in :leiningen
  :plugins [[environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks])
