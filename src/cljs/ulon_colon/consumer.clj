;; ulon-colon consumer Clojure Script
(ns ulon-colon.consumer)


(defn- reconnect [consumer]
  (let [client (js/WebSocket. (@consumer :producer-url)})]
    (logging/info (str "Reconnect to producer" (@consumer :producer-url)))
    (swap! consumer assoc :channel (wait-for-result client))))

(defn make-consumer [producer-url]
  (let [ws (js/WebSocket. producer-url)
        consumer (atom {:channel ws :producer-url producer-url})]
    (swap! consumer assoc :on-drained (partial reconnect consumer))
    (.onclose  ch (@consumer :on-drained))
    consumer ws))