(ns eu.probee.website.core
  (:use [compojure.core])
  (:require [compojure.route :as route]
            [compojure.handler :refer [api]]
            [noir.session :refer [wrap-noir-session]]
            [eu.probee.website.pages :refer [page-routes]]
            [eu.probee.website.admin :refer [admin-routes]]))

(defroutes app-routes
  admin-routes
  page-routes
  (route/resources "/")
  (route/not-found "Page not found"))

(def ring-handler
  (-> app-routes
      api
      wrap-noir-session))
