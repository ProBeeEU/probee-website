(defproject website "0.1.0-SNAPSHOT"
  :description "The main website for ProBee"
  :url "http://probee.eu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.7.0"]
                 [com.novemberain/monger "1.7.0"]
                 [lib-noir "0.8.3"]
                 [compojure "1.1.7"]
                 [enlive "1.1.5"]
                 [slugger "1.0.1"]
                 [markdown-clj "0.9.43"]
                 [com.taoensso/timbre "3.2.1"]
                 [com.postspectacular/rotor "0.1.0"]]
  :source-paths ["src/clj"]
  :min-lein-version "2.0.0")
