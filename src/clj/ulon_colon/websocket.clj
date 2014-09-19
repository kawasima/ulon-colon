(ns ulon-colon.websocket
  (:import [io.netty.bootstrap Bootstrap ServerBootstrap]
           [io.netty.channel Channel ChannelInitializer]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket.nio NioSocketChannel NioServerSocketChannel]
           [io.netty.handler.codec.http DefaultHttpHeaders HttpClientCodec HttpObjectAggregator]
           [io.netty.handler.codec.http.websocketx WebSocketVersion WebSocketClientHandshakerFactory]
           [io.netty.handler.ssl SslContext]))

(def websocket-client-handler
  (proxy [SimpleChannelInboundHandler] []
    (handlerAdded [ctx]
      (.newPromise ctx))
    (channelActive [ctx])))
(defn url->options [url]
  (when url
    (let [url (java.net.URI. url)
          path (.getPath url)]
      {:scheme (.getScheme url)
       :server-name (.getHost url)
       :server-port (.getPort url)
       :uri (if (empty? path) "/" path)
       :user-info (.getUserInfo url)
       :query-string (.getQuery url)})))

(def ssl-ctx nil)

(defn- create-bootstrap [handler host port group]
  (.. (Bootstrap.)
    (group group)
    (channel NioSocketChannel)
    (handler (proxy [ChannelInitializer] []
               (initChannel [ch]
                 (let [p (.pipeline ch)]
                   (when ssl-ctx
                     (.addLast p (.newHandler ssl-ctx (.alloc ch) host port)))
                   (.addLast p
                     (HttpClientCodec.)
                     (HttpObjectAggregator. 8192)
                     handler)))))))

#_(defn websocket-server [port]
  (let [boss-group (NioEventLoopGroup. 1)
        worker-group (NioEventLoopGroup.)
        b (ServerBootstrap.)]
    (try
      (.. b
        (group boss-group worker-group)
        (channel NioServerSocketChannel)
        (handler handler)
        (childHandler (WebSocketServerInitializer. nil)))
      (.. b
        (bind port) sync channel
        closeFuture
        sync)
      (finally
        (.shutdownGracefully boss-group)
        (.shutdownGracefully worker-group)))))

(defn websocket-client [url]
  (let [options (url->options url)
        handler (WebSocketClientHandler.
                  (WebSocketClientHandlerFactory/newHandshaker
                    url WebSocketVersion/V13 nil false (DefaultHttpHeaders.)))
        ch (.. b
             (connect (:server-name options) (:server-port options))
             sync
             channel)]
    (.. handler handshakeFuture sync)
    
    ))
