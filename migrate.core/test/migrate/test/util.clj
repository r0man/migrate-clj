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
  (is (empty? (re-ns-matches #"UNKNWON-NAMESPACE")))
  (is (= '[migrate.test.util] (re-ns-matches #"migrate.test.util"))))

(deftest test-resolve-db-spec
  (are [db-spec expected]
    (is (=  expected (resolve-db-spec db-spec)))
    nil nil
    "" ""
    "x" "x"
    :migrate-db "postgresql://localhost/migrate_development"
    ":migrate-db" "postgresql://localhost/migrate_development"
    "postgresql://localhost/migrate_development" "postgresql://localhost/migrate_development"))

(deftest test-resolve-var
  (is (thrown? AssertionError (resolve-var 'migrate.test.util 'unknown-var)))
  (let [v (resolve-var 'migrate.util 'resolve-var)]
    (is (= #'resolve-var (:var v)))
    (is (= (:doc (meta #'resolve-var)) (:doc v)))))