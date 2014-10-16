(ns ulon-colon.consumer
  (:require [clojure.tools.logging :as logging]
            [clojure.core.async :as async :refer [chan go go-loop <! <!! put!]]
            [clojure.data.fressian :as fress])
  (:import  [net.unit8.uloncolon WebSocketClient WebSocketMessageListener]))

(defn make-consumer [producer-url]
  (let [ch (chan)
        client (WebSocketClient.
                 (proxy [WebSocketMessageListener] []
                   (onBinaryMessage [_ message]
                     (put! ch message))))]
    (.connect client producer-url)
    {:client client :channel ch}))

(defn- consume* [client consume-fn msg-raw]
  (let [msg (-> msg-raw  fress/read)
        res (atom {:id (msg :id)})]
    (try (consume-fn (msg :body))
      (swap! res assoc :status :ack)
      (catch Exception ex
        (swap! res assoc :status :fail, :cause (.getMessage ex))
        (throw ex))
      (finally
       (.send client (fress/write @res))))))

(defn consume [consumer consume-fn & {:keys [on-fail]}]
  (go-loop []
    (let [msg-raw (<! (:channel consumer))]
      (try
        (consume* (:client consumer) consume-fn msg-raw)
        (catch Exception ex
          (when on-fail (on-fail))))
      (recur))))

(defn consume-sync
  "Consume message in synchronized mode. If message "
  [consumer consume-fn]
  (let [msg-raw (<!! (:channel consumer))]
    (consume* (:client consumer) consume-fn msg-raw)))
      

(defn stop-consume [consumer]
  (.close (:client consumer)))

