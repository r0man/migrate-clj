(ns migrate.test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest]]
            [migrate.sql :refer [create-migration-table drop-migration-table]]
            [migrate.connection :refer [with-connection]]))

(def test-db :migrate-db)

(defmacro with-version-table [& body]
  `(try (do (create-migration-table) ~@body)
        (finally
         (try (drop-migration-table)
              (catch Exception e# nil)))))

(defmacro dbtest [name & body]
  `(deftest ~name
     (with-connection test-db
       (jdbc/transaction
        (try
          (with-version-table
            ~@body)
          (finally (jdbc/set-rollback-only)))))))
