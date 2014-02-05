(ns ulon-colon.browser.core
  (:use [compojure.core]
        [hiccup.core]
        [hiccup.page :only [html5]]
        [ulon-colon.producer])
  (:require [compojure.route :as route]))

(defn view-images []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:script {:src "/js/example.js"}]]
   [:body
    [:canvas {:id "image-1" :width "320" :height "240"}]]))

(start-producer)

(defroutes app
  (GET "/" [] (view-images))
  (route/resources "/"))

