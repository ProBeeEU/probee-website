(ns eu.probee.website.config
  (:require [clojure.edn :as edn]))

(def settings (atom {}))

(defn init [file-path]
  (reset! settings (try (edn/read-string (slurp file-path))
                        (catch Exception e
                          (println (.getMessage e))))))

