(defproject migrate/migrate "0.0.7-SNAPSHOT"
  :description "Rails-like Database Migrations for Clojure"
  :url "http://github.com/r0man/migrate-clj"
  :autodoc {:name "Rails-like Database Migrations for Clojure",
            :author "Roman Scherer",
            :web-src-dir "http://github.com/r0man/migrate-clj/blob/",
            :web-home "http://github.com/r0man/migrate-clj",
            :copyright "Copyright (c) 2010 Roman Scherer"}
  :dependencies [[lein-env "0.0.3-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.1.4"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]
                 [postgresql/postgresql "9.1-901.jdbc4"]]
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[leiningen/leiningen "2.0.0-preview2"]
                                  [org.clojure/java.jdbc "0.1.4"]
                                  [org.slf4j/slf4j-log4j12 "1.5.6"]
                                  [postgresql/postgresql "9.1-901.jdbc4"]]}}
  :plugins [[lein-env "0.0.3-SNAPSHOT"]]
  :hooks [leiningen.hooks.env]
  :min-lein-version "2.0.0"
  :migrate [migrate.examples])
