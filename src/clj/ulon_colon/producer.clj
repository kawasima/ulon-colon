(ns ulon-colon.producer
  (:require [clojure.tools.logging :as logging]
            [lamina.core :refer :all]
            [aleph.http :refer :all]
            [clojure.data.fressian :as fress]))

(def broadcast-channel (channel))
(def produce-channel (channel))
(def transactions (atom {}))

(defn- response-handler [ch msg-raw]
  (let [msg (-> msg-raw .array fress/read)
        msg-id (msg :id)]
    (if (= (msg :status) :ack)
      (do
        (logging/info (str "Receive Ack from " ch))
        (logging/info (str "Transaction count: "  (count (get-in @transactions [msg-id :consumers]))))
        (swap! transactions update-in [msg-id :consumers] #(remove (partial = %2) %1) ch)
        (when (empty? (get-in @transactions [msg-id :consumers]))
          (logging/info (str "Transaction commit: " msg-id))
          (enqueue (get-in @transactions [msg-id :producer]) :commit)
          (swap! transactions dissoc msg-id))))))

(defn- handler [ch handshake]
  (let [receive-cb (partial response-handler ch)]
    (receive-all ch receive-cb)
    (on-closed ch (fn []
                    (logging/debug "Closed consumer:" ch)
                    (cancel-callback ch receive-cb))))
  (receive-all broadcast-channel (fn [msg]
                                   (swap! transactions update-in [(msg :id) :consumers] conj ch)
                                   (logging/info (str "Added transactions: " (msg :id) @transactions))
                                   (enqueue ch (msg :payload)))))

(defn start-producer [& {:keys [port] :or {port 5629}}]
  (receive-all produce-channel
               (fn [msg] (enqueue broadcast-channel msg)))
  (start-http-server handler
                     {:port port
                      :websocket true}))

(defn produce [msg]
  (let [message-id (java.util.UUID/randomUUID)
        response-queue (channel)]
    (swap! transactions assoc message-id
           {:producer response-queue :consumers []})
    (enqueue produce-channel {:id message-id
                               :payload (->> {:id message-id :body msg}
                                          fress/write)})
    response-queue))

(defn -main [& args])