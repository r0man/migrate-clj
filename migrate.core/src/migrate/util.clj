(ns migrate.util
  (:import org.joda.time.format.DateTimeFormat
           org.joda.time.DateTimeZone)
  (:refer-clojure :exclude [replace])
  (:require [environ.core :refer [env]]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-date-time to-long]]
            [clj-time.format :refer [formatters unparse parse]]
            [clojure.string :refer [replace split]]
            [clojure.java.classpath :refer [classpath]]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [inflections.util :refer [parse-integer parse-url]]))

(def ^:dynamic *version-regex*
  #".*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*")

(defn find-base-ns
  "Find the base migration namespace for `db-name` in `project`."
  [project db-name]
  (get (:migrations project) (keyword db-name)))

(defn format-time
  "Format `time` using the YYYYMMddHHmmss pattern."
  [time] (unparse (DateTimeFormat/forPattern "YYYYMMddHHmmss") (to-date-time time)))

(defn format-human-time
  "Format `time` in a human readable format."
  [time] (unparse (formatters :rfc822) (to-date-time time)))

(defn format-subname
  "Format the database spec subname from `url`."
  [url]
  (let [{:keys [server-name server-port query-string]} url]
    (str "//" server-name (if server-port (str ":" server-port))
         (:uri url) (if query-string (str "?" query-string))))  )

(defn parse-db-spec
  "Parse `s` as a database spec return a clojure/java.jdbc and Korma compatible map."
  [s]
  (if-let [url (parse-url s)]
    {:subprotocol (:scheme url)
     :user (:user url)
     :password (:password url)
     :subname (format-subname url)
     :host (:server-name url)
     :port (:server-port url)
     :db (replace (:uri url) #"^/" "")}))

(defn parse-time
  "Format `s` as time using the YYYYMMddHHmmss pattern."
  [s]
  (if-let [matches (re-matches #".*(\d{14}).*" (str s))]
    (.withZoneRetainFields
     (parse (DateTimeFormat/forPattern "YYYYMMddHHmmss") (second matches))
     DateTimeZone/UTC)))

(defn parse-version
  "Parse the version timestamp from the namespace `ns`."
  [ns]
  (let [[year month day hour minute second]
        (map parse-integer (rest (re-matches *version-regex* (str ns))))]
    (if (and year month day hour minute second)
      (date-time year month day hour minute second))))

(defn re-ns-matches
  "Finds all namespaces on the classpath matching `re`."
  [re] (filter #(re-matches re (str %1)) (find-namespaces (classpath))))

(defmacro with-base-ns
  "Eval `body` with `sym` bound to the migration base namespace for `db-name`."
  [[project db-name sym] & body]
  `(let [db-name# ~db-name]
     (if-let [~sym (find-base-ns ~project db-name#)]
       (do ~@body)
       (do (println (format "Can't find migration base ns in project.clj for db %s." db-name#))
           (System/exit 1)))))