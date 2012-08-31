(ns migrate.util
  (:refer-clojure :exclude [replace])
  (:require [environ.core :refer [env]]
            [clj-time.core :refer [date-time]]
            [clojure.string :refer [replace split]]
            [clojure.java.classpath :refer [classpath]]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [inflections.util :refer [parse-integer parse-url]]))

(def ^:dynamic *version-regex*
  #".*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*")

(defn parse-db-spec
  "Parse `s` as a database spec return a clojure/java.jdbc and Korma compatible map."
  [s]
  (if-let [{:keys [server-name server-port query-string] :as url}
           (parse-url s)]
    {:subprotocol (:scheme url)
     :user (:user url)
     :password (:password url)
     :subname (str "//" server-name
                   (if server-port
                     (str ":" server-port))
                   (:uri url) (if query-string (str "?" query-string)))
     :host server-name
     :port server-port
     :db (replace (:uri url) #"^/" "")}))

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

(defn resolve-db-spec
  "Reolve `db-spec` via environ or return `db-spec`."
  [db-spec]
  (cond
   (keyword? db-spec)
   (env db-spec)
   (and (string? db-spec) (= \: (first db-spec)))
   (env (keyword (apply str (rest db-spec))))
   :else db-spec))

(defn resolve-var
  "Resolve `var` in `ns` and return a map with :var and :doc keys. If
  `var` can't be found thrown an AssertionError."
  [ns var]
  (if-let [v (ns-resolve ns var)]
    {:var v :doc (:doc (meta v))}
    (throw (AssertionError. (format "Can't resolve var #'%s in ns %s." var ns)))))
