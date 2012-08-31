(ns migrate.test.util
  (:require [clj-time.core :refer [date-time]]
            [clojure.test :refer :all]
            [migrate.util :refer :all]))

(deftest test-parse-version
  (is (nil? (parse-version nil)))
  (is (nil? (parse-version "")))
  (is (= (date-time 2012 8 17 14 29)
         (parse-version 'migrate.db.test.20120817142900-create-continents-table))))