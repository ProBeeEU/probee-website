(ns eu.probee.website.database
  (:refer-clojure :exclude [get set remove])
  (:use [monger.operators])
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.joda-time]
            [clj-time.core :as dt]
            [eu.probee.website.util :as util])
  (:import [org.bson.types ObjectId]))

(defn connect [{:keys [db user pwd]}]
  "Takes a configuration map and connects to the specified mongo database"
  (mg/connect!)
  (mg/use-db! db)
  (when (and user pwd)
    (mg/authenticate user (.toCharArray pwd))))

(defn insert
  [table data]
  (let [id (ObjectId.)]
    (mc/insert table (assoc data :_id id))
    id))

(defn update
  [table oid data]
  (mc/update table {:_id (ObjectId. oid)} {$set data})
  oid)

(defn get [table oid]
  (mc/find-one-as-map table {:_id (ObjectId. oid)}))

(defn get-by-created [type]
  (reverse (sort-by :created (mc/find-maps type))))

(defn get-by-released [type]
  (reverse (sort-by :released (mc/find-maps type))))

(defn get-by-slug-name [type name]
  (mc/find-one-as-map type {:slug-name name}))

(defn get-by-order [type]
  (sort #(< (util/ensure-int (:order %1)) (util/ensure-int (:order %2)))
        (mc/find-maps type)))

(defn get-menu-items
  []
  (get-by-order "menu"))

(defn get-page-by-name
  [name]
  (mc/find-one-as-map "page" {:name name}))
