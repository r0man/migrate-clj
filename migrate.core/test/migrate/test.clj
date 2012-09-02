(ns migrate.test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest]]
            [migrate.sql :refer :all]))

(def test-db-spec "postgresql://localhost/migrate_test")

(defmacro with-version-table [& body]
  `(try (do (create-migration-table *migration-table*) ~@body)
        (finally
         (try (drop-migration-table *migration-table*)
              (catch Exception e# nil)))))

(defmacro dbtest [name & body]
  `(deftest ~name
     (with-connection test-db-spec
       (jdbc/transaction
        (try
          (with-version-table
            ~@body)
          (finally (jdbc/set-rollback-only)))))))
