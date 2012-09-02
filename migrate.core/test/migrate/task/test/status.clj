(ns migrate.task.test.status
  (:require [clojure.test :refer :all]
            [migrate.task.status :refer :all]
            [migrate.core :refer [latest-migration run]]
            [migrate.sql :refer [select-current-version *migration-table*]]
            [migrate.test :refer [dbtest]]))

(dbtest test-print-migrations
  (status 'migrate.example)
  (run 'migrate.example)
  (is (= (:version (latest-migration 'migrate.example))
         (select-current-version *migration-table*)))
  (status 'migrate.example))