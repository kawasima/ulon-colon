(ns ulon-colon.browser.core
  (:use [incanter core charts]
        [flume-node.core :as flume :only [defsink defsource defagent]]
        [ring.adapter.jetty :only [run-jetty]]
        [compojure.core]
        [hiccup.core]
        [hiccup.page :only [html5]]
        [ulon-colon.producer])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [clojure.string :as string]
            [clojure.data.fressian :as fress])
  (:import  [clojure.lang PersistentQueue]
            [java.io FileReader BufferedReader]
            [org.apache.flume.event EventBuilder]))


(defsource proc-stat-source
  :process (fn []
             (Thread/sleep 3000)
             (with-open [rdr (-> (FileReader. "/proc/stat") (BufferedReader.))]
               (let [tokens (-> (.readLine rdr) (string/split #"\s+"))]
                 (EventBuilder/withBody (->> tokens
                                             (drop 1)
                                             (map #(Long/parseLong %))
                                             fress/write
                                             .array))))))

(defn- to-percentage [s]
  (let [total (reduce + s)]
    (map #(double (/ % total)) s)))

(defn- make-graph-generator []
  (let [timeline   (atom (PersistentQueue/EMPTY))
        cpu-usages (atom (PersistentQueue/EMPTY))
        last-stats (atom nil)]
    (fn [event]
      (let [baos (java.io.ByteArrayOutputStream.)
            dt (System/currentTimeMillis)
            stats (fress/read (.getBody event))]
        (when @last-stats
          (swap! timeline conj dt)
          (swap! cpu-usages conj
            (->> (interleave stats (or @last-stats stats))
              (partition-all 2)
              (map #(apply - %))
              (to-percentage)))
          (-> (time-series-plot @timeline (map first @cpu-usages)
                :x-label "Date"
                :y-label "Percentage"
                :series-label "user"
                :legend true)
            (add-lines @timeline (map #(nth % 1) @cpu-usages) :series-label "nice")
            (add-lines @timeline (map #(nth % 2) @cpu-usages) :series-label "sys")
            (add-lines @timeline (map #(nth % 3) @cpu-usages) :series-label "idle")
            (save baos))
          (produce {:image (.toByteArray baos)}))
        (reset! last-stats stats)))))

(defsink incanter-sink
  :start   (fn [] (start-producer))
  :process (make-graph-generator))

(defn view-images []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:script {:src "/js/jquery-1.11.0.min.js"}]
    [:script {:src "/js/example.js"}]]
   [:body
     [:h1 "CPU usages"]
     [:p "Showing real-time CPU usages"]
     [:canvas {:id "image-1" :width "640" :height "480"}]]))

(defroutes app
  (GET "/" [] (view-images))
  (route/resources "/"))

(defagent :a1
  (flume/source :r1
    :type "ulon-colon.browser.core/proc-stat-source"
    :channels :c1)
  (flume/sink :k1
    :type "ulon-colon.browser.core/incanter-sink"
    :channel :c1)
  (flume/channel :c1
    :type "memory"
    :capacity 1000
    :transactionCapacity 100))

(defn -main [& args]
  (flume/make-app)
  (run-jetty (-> app handler/site reload/wrap-reload) {:port 3000}))
