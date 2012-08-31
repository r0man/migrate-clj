(defproject migrate/migrate "0.1.0-SNAPSHOT"
  :description "Library for managing database migrations"
  :url "http://github.com/r0man/migrate-clj"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :plugins [[lein-sub "0.2.0"]]
  :sub ["migrate.core" "migrate.lein" "migrate.example"])