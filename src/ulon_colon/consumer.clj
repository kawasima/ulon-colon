(ns ulon-colon.consumer
  (:require [clojure.tools.logging :as logging]
            [lamina.core :refer :all]
            [aleph.http :refer :all]
            [clojure.data.fressian :as fress]))

(defn- reconnect [consumer]
  (let [client (websocket-client {:url (@consumer :producer-url)})]
    (logging/info (str "Reconnect to producer" (@consumer :producer-url)))
    (swap! consumer assoc :channel (wait-for-result client))))

(defn make-consumer [producer-url]
  (let [client (websocket-client {:url producer-url})
        ch (wait-for-result client)
        consumer (atom {:channel ch :producer-url producer-url})]
    (swap! consumer assoc :on-drained (partial reconnect consumer))
    (on-drained ch (@consumer :on-drained))
    consumer))

(defn- consume* [ch consume-fn msg-raw]
  (let [msg (-> msg-raw .array fress/read)
        res (atom {:id (msg :id)})]
    (try (consume-fn (msg :body))
      (swap! res assoc :status :ack)
      (catch Exception ex
        (swap! res assoc :status :fail, :cause (.getMessage ex))
        (throw ex))
      (finally
       (enqueue ch (fress/write @res))))))

(defn consume [consumer consume-fn & {:keys [on-fail]}]
  (let [recv-fn (fn [msg-raw]
                    (try
                      (consume* (@consumer :channel) consume-fn msg-raw)
                      (catch Exception ex
                        (when on-fail (on-fail)))))]
    (receive-all (@consumer :channel) recv-fn)
    (swap! consumer assoc :receive-all recv-fn)))

(defn consume-sync
  "Consume message in synchronized mode. If message "
  [consumer consume-fn]
  (let [ch (@consumer :channel)
        msg-raw @(read-channel ch)]
    (consume* (@consumer :channel) consume-fn msg-raw)))

(defn stop-consume! [consumer]
  (let [ch (@consumer :channel)]
    (some->> (@consumer :receive-all) (cancel-callback ch))
    (close ch)))

(defn -main [& args])
