(ns migrate.test.connection
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [migrate.connection :refer :all]))

(def db-spec "postgresql://localhost/migrate_test")

(deftest test-identifier-quote-string
  (with-connection db-spec
    (identifier-quote-string (jdbc/connection))))

(deftest test-resolve-db-spec
  (are [db-spec expected]
    (is (=  expected (resolve-db-spec db-spec)))
    nil nil
    "" ""
    "x" "x"
    :migrate-db "postgresql://localhost/migrate_development"
    ":migrate-db" "postgresql://localhost/migrate_development"
    "postgresql://localhost/migrate_development" "postgresql://localhost/migrate_development"))

(deftest test-with-connection
  (with-connection db-spec
    (is (jdbc/connection))
    (is (= "\"x\"" (@#'jdbc/*as-str* "x")))))
