(ns ulon-colon.producer
  (:require [clojure.tools.logging :as logging]
            [clojure.core.async :as async :refer [chan go-loop <! put! mult tap untap]]
            [clojure.data.fressian :as fress])
  (:import  [net.unit8.uloncolon WebSocketServer WebSocketServerHandler WebSocketServerHandlerFactory]
            [io.netty.buffer Unpooled]
            [io.netty.handler.codec.http.websocketx BinaryWebSocketFrame]))

(def produce-channel (chan))
(def broadcast-channel (mult produce-channel))
(def transactions (atom {}))

(defn- response-handler [channel msg-raw]
  (let [msg (-> msg-raw fress/read)
        msg-id (msg :id)]
    (if (= (msg :status) :ack)
      (do
        (logging/debug (str "Receive Ack from " channel))
        (swap! transactions update-in [msg-id :consumers] #(remove (partial = %2) %1) channel)
        (when (empty? (get-in @transactions [msg-id :consumers]))
          (logging/debug (str "Transaction commit: " msg-id))
          (put! (get-in @transactions [msg-id :producer]) :commit)
          (swap! transactions dissoc msg-id))))))

(defn start-producer [& {:keys [port] :or {port 5629}}]
  (WebSocketServer.
    (proxy [WebSocketServerHandlerFactory] []
      (create []
        (let [client-channel (chan)]
          (proxy [WebSocketServerHandler] []
            (onConnect [ctx]
              (tap broadcast-channel client-channel)
              (go-loop []
                (let [msg (<! client-channel)]
                  (swap! transactions update-in [(msg :id) :consumers] conj ctx)
                  (. ctx writeAndFlush (BinaryWebSocketFrame. (Unpooled/copiedBuffer (msg :payload))))
                  (if (.. ctx channel isActive)
                    (recur)
                    (untap broadcast-channel client-channel)))))
            (onBinaryMessage [ctx message]
              (response-handler ctx message))
            (onDisconnect [ctx]
              (untap broadcast-channel client-channel))))))
    port))
  
(defn produce [msg]
  (let [message-id (java.util.UUID/randomUUID)
        response-queue (chan)]
    (swap! transactions assoc message-id
           {:producer response-queue :consumers []})
    (logging/debug (str "produce " msg))
    (put! produce-channel {:id message-id
                           :payload (->> {:id message-id :body msg}
                                      fress/write)})
    response-queue))

(defn stop-producer [producer]
  (.close producer))
