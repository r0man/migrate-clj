(defproject migrate/migrate.lein "0.0.9-SNAPSHOT"
  :description "Leiningen plugin for Migrate"
  :url "http://github.com/r0man/migrate-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[environ/environ.lein "0.2.1"]
                 [migrate/migrate.core "0.0.9-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]]
  :eval-in-leiningen true)
