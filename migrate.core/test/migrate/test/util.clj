(ns migrate.test.util
  (:require [clj-time.core :refer [date-time]]
            [clojure.test :refer :all]
            [migrate.util :refer :all]))

(deftest test-find-base-ns
  (are [project db-name expected]
    (is (= expected (find-base-ns project db-name)))
    {:migrations {}}
    :example-db nil
    {:migrations {:example-db 'migrate.example.migrations}}
    :not-existing nil
    {:migrations {:example-db 'migrate.example.migrations}}
    :example-db 'migrate.example.migrations))

(deftest test-format-time
  (is (= "20120817142955" (format-time (date-time 2012 8 17 14 29 55)))))

(deftest test-format-human-time
  (is (= "Fri, 17 Aug 2012 14:29:55" (format-human-time (date-time 2012 8 17 14 29 55)))))

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

(deftest test-parse-time
  (is (nil? (parse-time nil)))
  (is (nil? (parse-time "")))
  (is (= (date-time 2012 8 17 14 29)
         (parse-time 'migrate.db.test.20120817142900-create-continents-table))))
