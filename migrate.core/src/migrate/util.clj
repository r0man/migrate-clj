(ns migrate.util
  (:require [clj-time.core :refer [date-time]]
            [inflections.number :refer [parse-integer]]))

(defn parse-version
  "Parse the version timestamp from a namespace."
  [ns]
  (let [[year month day hour minute second]
        (map parse-integer (rest (re-matches #".*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*"(str ns))))]
    (if (and year month day hour minute second)
      (date-time year month day hour minute second))))
