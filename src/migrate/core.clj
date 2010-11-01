(ns migrate.core
  (:import java.io.File [java.sql DriverManager SQLException])
  (:refer-clojure :exclude (replace))
  (:require [clojure.contrib.sql :as sql])
  (:use [clojure.string :only (replace)]
        [clojure.contrib.def :only (defvar)]
        [clojure.contrib.logging :only (info)]))

(defvar *migrations*
  (atom {}) "The defined migrations by name.")

(defvar *version-table*
  "schema_versions" "The table name for the schema versions.")

(defn str= [s1 s2] (= (.compareTo s1 s2) 0))
(defn str< [s1 s2] (< (.compareTo s1 s2) 0))
(defn str> [s1 s2] (> (.compareTo s1 s2) 0))
(defn str<= [s1 s2] (<= (.compareTo s1 s2) 0))
(defn str>= [s1 s2] (>= (.compareTo s1 s2) 0))

(defn create-version-table
  "Creates the schema version table."
  [] (sql/create-table *version-table* [:id :string "PRIMARY KEY"]))

(defmacro defmigration [name doc-string up-fn down-fn]
  `(let [migration# {:name ~name :doc ~doc-string :up #(~@up-fn) :down #(~@down-fn)}]
     (swap! *migrations* assoc (:name migration#) migration#)))

(defn drop-version-table
  "Drop the schema version table."
  [] (sql/drop-table *version-table*))

(defn find-applicable [from-version to-version]
  (if (str<= from-version to-version)
    (filter #(and (str< from-version (:name %)) (str<= (:name %) to-version))
            (sort-by :name (vals @*migrations*)))
    (reverse (find-applicable to-version from-version))))

(defn select-current-version
  "Returns the current schema version."
  [] (sql/with-query-results result-set
       [(str "SELECT MAX(id) AS version FROM " *version-table*)]
       (:version (first result-set))))

(defn version-table? []
  "Returns true if the *version-table* exists, otherwise false."
  (try (do (select-current-version) true)
       (catch SQLException _ false)))

(defn run []
  (let [current (or (select-current-version) "")
        target (last (sort (keys @*migrations*)))
        direction (if (str<= current target ) :up :down)]
    (doseq [migration (find-applicable current target)]
      (info (str "Running migration: " (:name migration)))
      (info (str "      Description: " (:doc migration)))
      ((direction migration)))))

