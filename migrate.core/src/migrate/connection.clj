(ns migrate.connection
  (:require [clojure.java.jdbc :as jdbc]
            [migrate.util :refer [resolve-db-spec]]))

(defn identifier-quote-string
  "Returns the string to quote identifiers from the `connection` meta data."
  [connection] (.getIdentifierQuoteString (.getMetaData (jdbc/connection))))

(defmacro with-connection
  "Evaluates body in the context of a new connection to a database."
  [db-spec & body]
  `(jdbc/with-connection (resolve-db-spec ~db-spec)
     (jdbc/with-quoted-identifiers (identifier-quote-string (jdbc/connection))
       ~@body)))
