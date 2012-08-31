(ns migrate.connection
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]))

(defn identifier-quote-string
  "Returns the string to quote identifiers from the `connection` meta data."
  [connection] (.getIdentifierQuoteString (.getMetaData (jdbc/connection))))

(defmacro with-connection [db-spec & body]
  `(jdbc/with-connection ~db-spec
     (jdbc/with-quoted-identifiers (identifier-quote-string (jdbc/connection)))
     ~@body))

;; (with-connection :bs-database
;;   (identifier-quote-string (jdbc/connection)))

;; (identifier-quote-string (jdbc/connection))

;; (java.net.URI. "postgresql://burningswell:burningswell13+@localhost/burningswell_development")
;; (java.net.URI. "postgresql")

;; (prn (env :bs-database))