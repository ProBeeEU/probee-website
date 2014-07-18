(ns immutant.init
  (:use eu.probee.website.core)
  (:require [immutant.web :as web]
            [immutant.jobs :as jobs]
            [monger.core :as mg]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]
            [eu.probee.website.config :as conf]
            [eu.probee.website.database :as db]))

;; This file will be loaded when the application is deployed to Immutant, and
;; can be used to start services your app needs.

(conf/init "/etc/immutant/website.edn")

(timbre/set-config! [:appenders :standard-out :enabled?] false)

(timbre/set-config!
 [:appenders :rotor]
 {:min-level :info,
  :enabled? true,
  :async? false,
  :max-message-per-msecs nil,
  :fn rotor/append})

(timbre/set-config!
 [:shared-appender-config :rotor]
 {:path (:log-path @conf/settings), :max-size (* 512 1024), :backlog 10})

(db/connect (:mongo @conf/settings))

(web/start #'eu.probee.website.core/ring-handler)

(timbre/info "ProBee website started successfully")
