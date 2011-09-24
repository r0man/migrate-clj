(defproject migrate "0.0.3"
  :description "Rails-like Database Migrations for Clojure"
  :autodoc {:name "Rails-like Database Migrations for Clojure"
            :author "Roman Scherer"
            :web-src-dir "http://github.com/r0man/migrate-clj/blob/"
            :web-home "http://github.com/r0man/migrate-clj"
            :copyright "Copyright (c) 2010 Roman Scherer"}
  :url "http://github.com/r0man/migrate-clj"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.0.5"]
                 [org.clojure/tools.logging "0.2.0"]]
  :dev-dependencies [[org.slf4j/slf4j-log4j12 "1.5.10"]
                     [org.xerial/sqlite-jdbc "3.7.2"]
                     [postgresql/postgresql "9.0-801.jdbc4"]]
  :multi-deps {"1.2.1" [[org.clojure/clojure "1.2.1"]
                        [org.clojure/java.jdbc "0.0.5"]
                        [org.clojure/tools.logging "0.2.0"]]})
