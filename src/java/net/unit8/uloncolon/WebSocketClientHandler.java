package net.unit8.uloncolon;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

/**
 * @author kawasima
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private DisconnectListener disconnectListener;
    private WebSocketMessageListener messageListener;

    public WebSocketClientHandler(String url) {
        URI uri = URI.create(url);
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), 65535 * 256);
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    /**
     * reconnect to server.
     *
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        disconnectListener.onDisconnect();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);

            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.getStatus() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf rawMsg = frame.content();
            byte[] buf = new byte[rawMsg.readableBytes()];
            rawMsg.readBytes(buf);
            messageListener.onBinaryMessage(ctx, buf);
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
        } else {
            throw new IllegalStateException("Unknown frame type: " + frame.getClass());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }

    public void setMessageListener(WebSocketMessageListener listener) {
        this.messageListener = listener;
    }
}
