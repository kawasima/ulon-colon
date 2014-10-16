package net.unit8.uloncolon;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author kawasima
 */
public class WebSocketClient {
    private EventLoopGroup group;
    private Channel channel;
    private WebSocketMessageListener messageListener;

    public WebSocketClient(WebSocketMessageListener listener) {
        this.messageListener = listener;
        this.group = new NioEventLoopGroup();
    }

    public void connect(final String url) {
        URI uri = URI.create(url);

        String scheme = uri.getScheme() == null? "http" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        try {
            if (ssl) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } else {
                sslCtx = null;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClient self = this;

            while(true) {
                Bootstrap b = new Bootstrap();
                final WebSocketClientHandler handler = new WebSocketClientHandler(url);

                b.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline p = ch.pipeline();
                                if (sslCtx != null) {
                                    p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                                }
                                p.addLast(
                                        new HttpClientCodec(),
                                        new HttpObjectAggregator(8192),
                                        handler);
                            }
                        });

                try {
                    ChannelFuture future = b.connect(uri.getHost(), port).sync();
                    if (future.isSuccess()) {
                        channel = future.channel();
                        handler.setDisconnectListener(new DisconnectListener() {
                            @Override
                            public void onDisconnect() {
                                channel.close();
                                self.connect(url);
                            }
                        });
                        handler.setMessageListener(messageListener);
                        handler.handshakeFuture().sync();
                        break;
                    }
                } catch(Exception e) {
                    // ignore
                }
                TimeUnit.SECONDS.sleep(5);
            }
        } catch(InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(byte[] msg) {
        if (channel != null && channel.isOpen()) {
            channel.writeAndFlush(
                    new BinaryWebSocketFrame(Unpooled.copiedBuffer(msg)));
        }
    }

    public void send(ByteBuffer msg) {
        if (channel != null && channel.isOpen()) {
            channel.writeAndFlush(
                    new BinaryWebSocketFrame(Unpooled.copiedBuffer(msg)));
        }
    }

    public void close() {
        try {
            channel.close();
        } finally {
            group.shutdownGracefully();
        }
    }
}
