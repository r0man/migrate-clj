(ns migrate.connection
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]))

(defn identifier-quote-string
  "Returns the string to quote identifiers from the `connection` meta data."
  [connection] (.getIdentifierQuoteString (.getMetaData (jdbc/connection))))

(defn resolve-db-spec
  "Reolve `db-spec` via environ or return `db-spec`."
  [db-spec]
  (cond
   (keyword? db-spec)
   (env db-spec)
   (and (string? db-spec)
        (= \: (first db-spec)))
   (env (keyword (apply str (rest db-spec))))
   :else db-spec))

(defmacro with-connection
  "Evaluates body in the context of a new connection to a database."
  [db-spec & body]
  `(jdbc/with-connection (resolve-db-spec ~db-spec)
     (jdbc/with-quoted-identifiers (identifier-quote-string (jdbc/connection))
       ~@body)))
