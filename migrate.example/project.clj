(defproject migrate/migrate.example "0.1.0-SNAPSHOT"
  :description "Migrate Example project"
  :url "https://github.com/r0man/migrate-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[environ "0.3.0"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]]
  :plugins [[environ/environ.lein "0.3.0"]]
  :hooks [environ.leiningen.hooks]
  :profiles {:dev {:env {:example-db "postgresql://localhost/example_test"}}}
  :migrations {:example-db migrate.example.migrations})
