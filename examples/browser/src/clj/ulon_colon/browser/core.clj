(ns ulon-colon.browser.core
  (:use [incanter core charts]
        [compojure.core]
        [hiccup.core]
        [hiccup.page :only [html5]]
        [ulon-colon.producer])
  (:require [compojure.route :as route]))


(defn view-images []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:script {:src "/js/jquery-1.11.0.min.js"}]
    [:script {:src "/js/example.js"}]]
   [:body
    [:canvas {:id "image-1" :width "320" :height "240"}]]))

(defn graph-generator []
  (let [producer (start-producer)]
    (while true
      (let [baos (java.io.ByteArrayOutputStream. )]
        (save (bar-chart ["a" "b" "c" "d" "e"]
                         (repeatedly 5 #(rand-int 50)))
              baos)
        (produce {:image (.toByteArray baos)}))
      (Thread/sleep 3000))))


(defroutes app
  (GET "/hello" [] (html [:p (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader)))]))
  (GET "/" [] (view-images))
  (GET "/monitor/start" []
       (.start (Thread. graph-generator))
       "OK")
  (route/resources "/"))

