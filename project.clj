(defproject migrate "0.0.6-SNAPSHOT"
  :description "Rails-like Database Migrations for Clojure"
  :autodoc {:name "Rails-like Database Migrations for Clojure"
            :author "Roman Scherer"
            :web-src-dir "http://github.com/r0man/migrate-clj/blob/"
            :web-home "http://github.com/r0man/migrate-clj"
            :copyright "Copyright (c) 2010 Roman Scherer"}
  :url "http://github.com/r0man/migrate-clj"
  :dependencies [[lein-env "0.0.2-SNAPSHOT"]
                 [org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.0.7"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.slf4j/slf4j-log4j12 "1.5.10"]]
  :dev-dependencies [[lein-env "0.0.2-SNAPSHOT"]
                     [leiningen/leiningen "1.6.2"]
                     [org.clojure/java.jdbc "0.0.7"]
                     [org.slf4j/slf4j-log4j12 "1.5.10"]
                     [org.xerial/sqlite-jdbc "3.7.2"]
                     [postgresql/postgresql "9.1-901.jdbc4"]]
  :hooks [leiningen.hooks.env]
  :migrate [migrate.examples])
