(ns eu.probee.website.admin
  (:refer-clojure :exclude [remove])
  (:use [net.cgrand.enlive-html :exclude [flatten]]
        [compojure.core]
        [slugger.core])
  (:require [markdown.core :as md]
            [immutant.util :refer [app-name]]
            [clj-time.format :as df]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [eu.probee.website.database :as db]
            [eu.probee.website.util :as util]
            [eu.probee.website.config :as conf]))

(defn wrap-admin-user-required [f]
  (fn [request]
    (if (= (session/get :user) "admin")
      (f request)
      {:status 403
       :body (str "Unauthorized request" (:session request))})))

(defn prepend-admin-uri [uri]
  (util/prepend-uri (str "admin/" uri)))

(deftemplate page "html-tpl/admin-index.html" [c]
  [[:link (attr? :href)]] (fn [node] (update-in node [:attrs :href] #(util/prepend-uri %)))
  [[:script (attr? :src)]] (fn [node] (update-in node [:attrs :src] #(util/prepend-uri %)))
  [[:img (attr? :src)]] (fn [node] (update-in node [:attrs :src] #(util/prepend-uri %)))
  [[:a (attr? :href)]] (fn [node] (update-in node [:attrs :href] #(util/prepend-uri %)))
  [:div#content] (html-content c))

(deftemplate login-page "/html-tpl/login.html" []
  [:form#action-form] (set-attr :action (prepend-admin-uri "")))

(defn get-login []
  (page (apply str (login-page))))

(defn handle-login [usr pwd]
  (let [{:keys [username password]} (:admin @conf/settings)]
    (if (and (= usr username) (= pwd password))
      (do (session/put! :user usr)
          (redirect (prepend-admin-uri "")))
      (get-login))))

(deftemplate index-page "html-tpl/admin.html" []
  [:#pages :div.page] (clone-for [page (db/get-by-released "page")]
                                    [:a] (do-> (set-attr :href (prepend-admin-uri (str "page?oid=" (:_id page))))
                                               (content (:name page))))
  [:#pages :a#new-page] (set-attr :href (prepend-admin-uri "page"))
  [:#menu-items :div.menu-item] (clone-for [menu (db/get-by-order "menu")]
                                           [:a] (do-> (set-attr :href (prepend-admin-uri (str "menu?oid=" (:_id menu))))
                                                      (content (:title menu))))
  [:#menu-items :a#new-menu-item] (set-attr :href (prepend-admin-uri "menu"))
  [:#blog-posts :div.blog-post] (clone-for [post (db/get-by-released "blog")]
                                              [:a] (do-> (set-attr :href (prepend-admin-uri (str "blog?oid=" (:_id post))))
                                                         (content (:title post))))
  [:#blog-posts :a#new-blog-post] (set-attr :href (prepend-admin-uri "blog"))
  [:#updates :div.update] (clone-for [update (db/get-by-released "update")]
                                        [:a] (do-> (set-attr :href (prepend-admin-uri (str "update?oid=" (:_id update))))
                                                   (content (:title update))))
  [:#updates :a#new-update] (set-attr :href (prepend-admin-uri "update"))
  [:#goals :div.goal] (clone-for [goal (db/get-by-order "goal")]
                                    [:a] (do-> (set-attr :href (prepend-admin-uri (str "goal?oid=" (:_id goal))))
                                               (content (:title goal))))
  [:#goals :a#new-goal] (set-attr :href (prepend-admin-uri "goal")))

(defsnippet edit-menu "html-tpl/edit.html" [:#edit-menu] [{:keys [oid title order page-name]}]
  [:input#title] (set-attr :value title)
  [:input#order] (set-attr :value order)
  [:input#page-name] (set-attr :value page-name))

(defn get-menu [oid]
  (page (apply str (emit* (edit-menu (when-not (nil? oid)
                                       (db/get "menu" oid)))))))

(defn update-menu [oid params]
  (let [data (select-keys params [:title :order :page-name])
        id (if (nil? oid)
             (db/insert "menu" data)
             (db/update "menu" oid data))]
    (redirect (str "menu?oid=" id))))

(deftemplate edit-page "html-tpl/edit-page.html" [{:keys [oid name] :as page}]
  [:input#name] (set-attr :value name)
  [:textarea#page-content] (content (:content page)))

(defn format-date
  [timestamp]
  (df/unparse (df/formatters :mysql) timestamp))

(deftemplate edit-post "html-tpl/edit-post.html" [type {:keys [oid title text released]}]
  [:input#title] (set-attr :value title)
  [:input#released] (set-attr :value (format-date released))
  [:textarea#text] (content text))

(deftemplate edit-goal "html-tpl/edit-goal.html" [{:keys [oid title order status text]}]
  [:input#title] (set-attr :value title)
  [:input#order] (set-attr :value order)
  [:textarea#text] (content text)
  [:select#status (attr= :value status)] (set-attr :selected "selected"))

(defn get-index []
  (page (apply str (index-page))))

(defn get-page [oid]
  (page (apply str (edit-page (when-not (nil? oid)
                                (db/get "page" oid))))))

(defn update-page [oid params]
  (let [data (select-keys params [:name :content])
        id (if (nil? oid)
             (db/insert "page" data)
             (db/update "page" oid data))]
    (redirect (str "page?oid=" id))))

(defn get-post [type oid]
  (page (apply str (edit-post type (when-not (nil? oid)
                                     (db/get type oid))))))

(defn parse-date [date-str]
  (df/parse (df/formatters :mysql) date-str))

(defn update-post [type oid params]
  (let [data (assoc (update-in (select-keys params [:title :text :released])
                               [:released] parse-date)
               :slug-name (->slug (:title params)))
        id (if (nil? oid)
             (db/insert type data)
             (db/update type oid data))]
    (redirect (str type "?oid=" id))))

(defn get-goal [oid]
  (page (apply str (edit-goal (when-not (nil? oid)
                                (db/get "goal" oid))))))

(defn update-goal [oid params]
  (let [data (select-keys params [:title :order :status :text])
        id (if (nil? oid)
             (db/insert "goal" data)
             (db/update "goal" oid data))]
    (redirect (str "goal?oid=" id))))

(defroutes restricted-routes
  (GET "/index" [] (get-index))
  (GET "/menu" [oid] (get-menu oid))
  (POST "/menu" [oid :as {params :params}] (update-menu oid params))
  (GET "/blog" [oid] (get-post "blog" oid))
  (POST "/blog" [oid :as {params :params}] (update-post "blog" oid params))
  (GET "/page" [oid] (get-page oid))
  (POST "/page" [oid :as {params :params}] (update-page oid params))
  (GET "/update" [oid] (get-post "update" oid))
  (POST "/update" [oid :as {params :params}] (update-post "update" oid params))
  (GET "/goal" [oid] (get-goal oid))
  (POST "/goal" [oid :as {params :params}] (update-goal oid params)))

(defroutes admin-routes
  (GET "/admin" [] (if-not (nil? (session/get :user)) (get-index) (get-login)))
  (GET "/admin/" [] (if-not (nil? (session/get :user)) (get-index) (get-login)))
  (POST "/admin/" [username password] (handle-login username password))
  (context "/admin" []
           (wrap-admin-user-required restricted-routes)))
