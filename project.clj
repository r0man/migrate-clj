(defproject migrate "0.0.1-SNAPSHOT"
  :description "Rails-like Database Migrations for Clojure"
  :autodoc {:name "Rails-like Database Migrations for Clojure"
            :author "Roman Scherer"
            :web-src-dir "http://github.com/r0man/migrate-clj/blob/"
            :web-home "http://github.com/r0man/migrate-clj"
            :copyright "Copyright (c) 2010 Roman Scherer"}
  :url "http://github.com/r0man/migrate-clj"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]
                     [log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                     [org.xerial/sqlite-jdbc "3.6.20.1"]
                     [postgresql/postgresql "8.4-701.jdbc4"]
                     [swank-clojure "1.2.1"]])
