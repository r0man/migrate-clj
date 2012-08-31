(defproject migrate/migrate.core "0.1.0-SNAPSHOT"
  :description "Core migration library for use in applications"
  :url "http://github.com/r0man/migrate-clj"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[environ "0.3.0"]
                 [clj-time "0.4.4"]
                 [inflections "0.7.2-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.classpath "0.2.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.namespace "0.2.0-SNAPSHOT"]]
  :profiles {:dev {:env {:migrate-db "postgresql://localhost/migrate_development"}
                   :dependencies [[migrate/example "0.1.0-SNAPSHOT"]
                                  [org.slf4j/slf4j-log4j12 "1.6.4"]
                                  [postgresql/postgresql "9.1-901.jdbc4"]]
                   :resource-paths ["test-resources"]}
             :test {:env {:migrate-db "postgresql://localhost/migrate_test"}}}
  :plugins [[environ/environ.lein "0.3.0"]]
  :hooks [environ.leiningen.hooks]
  :eval-in :leiningen)
