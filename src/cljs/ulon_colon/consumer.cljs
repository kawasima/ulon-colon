;; ulon-colon consumer Clojure Script
(ns ulon-colon.consumer
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [fressian-cljs.core :as fress]
            [cljs.core.async :refer [chan <! put!]]))

(def receive (chan))

(defn- reconnect [consumer]
  (let [client (js/WebSocket. (@consumer :producer-url))]
    (.log js/console (str "Reconnect to producer" (@consumer :producer-url)))))

(defn make-consumer [producer-url]
  (let [ws (js/WebSocket. producer-url)
        consumer (atom {:channel ws :producer-url producer-url})]
    (doto ws
      (aset "binaryType" "arraybuffer")
      (aset "onclose" #(reconnect consumer))
      (aset "onmessage"
            (fn [m]
              (when-let [data (fress/read (.-data m))]
                (put! receive data)))))
    consumer))

(defn- consume* [consumer consume-fn msg]
  (let [res (atom {:id (msg :id)})]
    (try (consume-fn (msg :body))
      (swap! res assoc :status :ack)
      (catch js/Error ex
        (swap! res assoc :status :fail, :cause (.getMessage ex))
        (throw ex))
      (finally
       (.send (:channel @consumer) (fress/write @res))))))

(defn consume [consumer consume-fn & {:keys [on-fail]}]
  (go
    (while true
      (let [msg (<! receive)]
        (consume* consumer consume-fn msg)))))

(defn consume-sync
  "Consume message in synchronized mode. If message "
  [consumer consume-fn]
  (go
   (let [msg (<! receive)]
     (consume* consumer consume-fn msg))))

(defn stop-consume! [consumer]
  (let [ws (@consumer :channel)]
    (set! (.-onmessage ws) nil)))
