(ns eu.probee.website.util
  (:require [immutant.registry :as registry]
            [eu.probee.website.config :as conf]))

(defn prepend-uri
  [href]
  (if (or (.contains href "//") (.contains href "mailto"))
    href
    (str (if (nil? (:context-path @conf/settings))
           (let [context-path (:context-path (registry/get :config))]
             (str (if (nil? context-path)
                    (str "/" (:name (registry/get :project)))
                    context-path) "/"))
           (:context-path @conf/settings)) href)))

(defn ensure-int [n]
  (if (integer? n)
    n
    (Integer/parseInt n)))
