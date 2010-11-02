(defproject migrate "0.0.1-SNAPSHOT"
  :description "Rails-like Database Migration for Clojure"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                     [org.xerial/sqlite-jdbc "3.6.20.1"]
                     [swank-clojure "1.2.1"]])
