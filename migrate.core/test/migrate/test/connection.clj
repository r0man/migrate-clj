(ns migrate.test.connection
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [migrate.connection :refer :all]))

(deftest test-identifier-quote-string
  (with-connection "postgresql://localhost/migrate_test"
    (identifier-quote-string (jdbc/connection))))
