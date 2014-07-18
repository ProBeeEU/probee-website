(ns eu.probee.website.pages
  (:refer-clojure :exclude [remove])
  (:use [net.cgrand.enlive-html :exclude [flatten]]
        [compojure.core]
        [slugger.core])
  (:require [markdown.core :as md]
            [clojure.string :as str]
            [immutant.util :refer [app-name]]
            [clj-time.format :as df]
            [eu.probee.website.database :as db]
            [eu.probee.website.util :as util]))

(defn format-date
  [timestamp]
  (df/unparse (df/formatter "MMM dd, yyyy") timestamp))

(def selected-page (atom "home"))

(deftemplate page "html-tpl/index.html" [c]
  [[:link (attr? :href)]] (fn [node] (update-in node [:attrs :href] #(util/prepend-uri %)))
  [[:script (attr? :src)]] (fn [node] (update-in node [:attrs :src] #(util/prepend-uri %)))
  [[:img (attr? :src)]] (fn [node] (update-in node [:attrs :src] #(util/prepend-uri %)))
  [[:a (attr? :href)]] (fn [node] (update-in node [:attrs :href] #(util/prepend-uri %)))
  [:body :> :div#container :> :div#content] (do-> (set-attr "data-page" @selected-page)
                                                  (html-content c))
  [:div#menu :> :span.menu-item] (clone-for [{:keys [title page-name]}
                                             (db/get-menu-items)]
                                            this-node (if (= @selected-page page-name)
                                                        (add-class "selected")
                                                        (fn [node] node))
                                            [:a] (do-> (set-attr :href (util/prepend-uri page-name))
                                                       (content title))))

(deftemplate home "html-tpl/home.html" []
  [:div.text] (html-content (md/md-to-html-string (:content (db/get-page-by-name "home"))))
  [:div#sidebar :div#recent-posts :div.no-posts] (if (empty? (db/get-by-released "blog"))
                                 (fn [node] node))
  [:div#sidebar :div.recent-post] (clone-for [{:keys [title released slug-name]}
                                              (take 5 (db/get-by-released "blog"))]
                                             [:a] (do-> (set-attr :href (util/prepend-uri (str "blog/" slug-name)))
                                                               (content title))
                                             [:span.date] (content (format-date released)))
  [:div#sidebar :div#recent-updates :div.no-posts] (if (empty? (db/get-by-released "update"))
                                                     (fn [node] node))
  [:div#sidebar :div.recent-update] (clone-for [{:keys [title released slug-name]}
                                                (take 5 (db/get-by-released "update"))]
                                               this-node (do-> (remove-class "recent-update")
                                                               (add-class "recent-post"))
                                               [:a] (do-> (set-attr :href (util/prepend-uri (str "update/" slug-name)))
                                                          (content title))
                                               [:span.date] (content (format-date released))))

(deftemplate generic "html-tpl/generic.html" [c]
  [:div.text] (html-content c))

(defn get-home []
  (reset! selected-page "home")
  (page (apply str (home))))

(defn get-page [name]
  (reset! selected-page name)
  (page (apply str (generic (md/md-to-html-string
                             (:content (db/get-page-by-name name)))))))

(defn get-summary [text]
  (first (str/split text #"<!--summary-->")))

(deftemplate post-list "html-tpl/post-list.html" [type]
  [:div.no-posts] (if (empty? (db/get-by-released type))
                    (fn [node] node))
  [:div.post] (clone-for [{:keys [title released slug-name text]}
                          (db/get-by-released type)]
                         [:h2] (content title)
                         [:span.date] (content (df/unparse (df/formatter "MMM dd, yyyy") released))
                         [:div.summary] (html-content (md/md-to-html-string (get-summary text)))
                         [:span.link :a] (set-attr :href (util/prepend-uri (str type "/" slug-name)))))

(defn get-post-list [type]
  (reset! selected-page type)
  (page (apply str (post-list type))))

(deftemplate post "html-tpl/post.html" [{:keys [title released text]}]
  [:h1] (content title)
  [:span.date] (content (df/unparse (df/formatter "MMM dd, yyyy") released))
  [:div.post-text] (html-content (md/md-to-html-string text)))

(defn get-post [type name]
  (reset! selected-page nil)
  (page (apply str (post (db/get-by-slug-name type name)))))

(deftemplate progress "html-tpl/progress.html" []
  [:div.goals :div.subtext] (html-content (md/md-to-html-string (:content (db/get-page-by-name "progress"))))
  [:div.goals :div.goal] (clone-for [{:keys [title text status]}
                                     (db/get-by-order "goal")]
                                    this-node (if status
                                                (add-class status)
                                                (fn [node] node))
                                    [:h2] (content title)
                                    [:div.description] (html-content (md/md-to-html-string text)))
  [:div#sidebar :div#recent-updates :div.no-posts] (if (empty? (db/get-by-released "update"))
                                                     (fn [node] node))
  [:div#sidebar :div.recent-post] (clone-for [{:keys [title released slug-name]}
                                               (take 10 (db/get-by-released "update"))]
                                              [:a] (do-> (set-attr :href (util/prepend-uri (str "update/" slug-name)))
                                                    (content title))
                                              [:span.date] (content (format-date released))))

(defn get-progress []
  (reset! selected-page "progress")
  (page (apply str (progress))))

(defroutes page-routes
  (GET "/" [] (get-home))
  (GET "/home" [] (get-home))
  (GET "/blog" [] (get-post-list "blog"))
  (GET "/progress" [] (get-progress))
  (GET "/update" [] (get-post-list "update"))
  (GET "/:page-name" [page-name] (get-page page-name))
  (GET "/blog/:post-title" [post-title] (get-post "blog" post-title))
  (GET "/update/:post-title" [post-title] (get-post "update" post-title)))
