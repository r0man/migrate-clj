(ns migrate.test.util
  (:require [clj-time.core :refer [date-time]]
            [clojure.test :refer :all]
            [migrate.util :refer :all]))

(deftest test-parse-version
  (is (nil? (parse-version nil)))
  (is (nil? (parse-version "")))
  (is (= (date-time 2012 8 17 14 29)
         (parse-version 'migrate.db.test.20120817142900-create-continents-table))))

(deftest test-re-ns-matches
  (is (empty? (re-ns-matches #"UNKNOWN-NAMESPACE")))
  (is (= '[migrate.test.util] (re-ns-matches #"migrate.test.util"))))

(deftest test-parse-db-spec
  (let [spec (parse-db-spec "postgresql://tiger:scotch@localhost/example")]
    (is (= "postgresql" (:subprotocol spec)))
    (is (= "tiger" (:user spec)))
    (is (= "scotch" (:password spec)))
    (is (= "//localhost/example" (:subname spec)))
    (is (= "localhost" (:host spec)))
    (is (nil? (:port spec)))
    (is (= "example" (:db spec))))
  (let [spec (parse-db-spec "postgresql://tiger:scotch@localhost:5432/migrate_test?a=1&b=2")]
    (is (= "postgresql" (:subprotocol spec)))
    (is (= "tiger" (:user spec)))
    (is (= "scotch" (:password spec)))
    (is (= "//localhost:5432/migrate_test?a=1&b=2" (:subname spec)))
    (is (= "localhost" (:host spec)))
    (is (= 5432 (:port spec)))
    (is (= "migrate_test" (:db spec)))))

(deftest test-resolve-db-spec
  (are [db-spec expected]
    (is (=  expected (resolve-db-spec db-spec)))
    nil nil
    "" ""
    "x" "x"
    :migrate-db "postgresql://localhost/migrate_test"
    ":migrate-db" "postgresql://localhost/migrate_test"
    "postgresql://localhost/migrate_test" "postgresql://localhost/migrate_test"))
