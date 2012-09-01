(ns migrate.task.test.new
  (:require [clj-time.core :refer [date-time now]]
            [clojure.test :refer :all]
            [clojure.java.io :refer [delete-file file]]
            [migrate.task.new :refer :all]))

(deftest test-template-ns
  (are [base-ns created-at description expected]
    (is (= expected (template-ns base-ns created-at description)))
    'migrate.example (date-time 2012 8 17 14 26) "create continents table"
    'migrate.example.20120817142600-create-continents-table))

(deftest test-template-filename
  (are [base-ns created-at description expected]
    (is (= expected (template-filename base-ns created-at description)))
    'migrate.example (date-time 2012 8 17 14 26) "create continents table"
    "migrate/example/20120817142600_create_continents_table.clj")
  (is (= "src/migrate/example/20120817142600_create_continents_table.clj"
         (template-filename "src" 'migrate.example (date-time 2012 8 17 14 26) "create continents table"))))

(deftest test-template
  (is (string? (template 'migrate.example (now) "create continents table"))))

(deftest test-create-template
  (let [file (file (create-template "/tmp" 'migrate.example "create continents table"))]
    (is (.exists file))
    (delete-file file)))