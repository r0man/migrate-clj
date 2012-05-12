(ns leiningen.migrate.hooks
  (:require [environ.leiningen.hooks :as environ]))

(defn activate []
  (environ/activate))