(defproject migrate/migrate.core "0.1.0-SNAPSHOT"
  :description "Core migration library for use in applications"
  :url "http://github.com/r0man/migrate-clj"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[clj-time "0.4.4"]
                 [commandline-clj "0.1.2"]
                 [environ "0.3.0"]
                 [inflections "0.7.2-SNAPSHOT"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/java.classpath "0.2.0"]
                 [org.clojure/java.jdbc "0.2.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.namespace "0.2.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[migrate/migrate.example "0.1.0-SNAPSHOT"]
                                  [org.slf4j/slf4j-log4j12 "1.6.4"]
                                  [postgresql/postgresql "9.1-901.jdbc4"]]}}
  ;; :plugins [[environ/environ.lein "0.3.0"]]
  ;; :hooks [environ.leiningen.hooks]
  ;; :aot [migrate.main]
  ;; :main migrate.main
  :eval-in :leiningen)
