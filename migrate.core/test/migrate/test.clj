(ns migrate.test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest]]
            [migrate.core :refer [create-migration-table drop-migration-table]]
            [migrate.connection :refer [with-connection]]))

(def test-db :migrate-db)

;; (defn cleanup-db []
;;   (doseq [table ["regions" "countries" "continents" "schema_migrations"]]
;;     (try (jdbc/do-commands (str "DROP TABLE IF EXISTS " table))
;;          (catch Exception _ nil))))

(defmacro dbtest [name & body]
  `(deftest ~name
     (with-connection test-db
       (jdbc/transaction
        (try ~@body
             (finally (jdbc/set-rollback-only)))))))

(defmacro with-version-table [& body]
  `(try (do (create-migration-table) ~@body)
        (finally (drop-migration-table))))
