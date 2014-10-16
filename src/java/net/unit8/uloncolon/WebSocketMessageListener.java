package net.unit8.uloncolon;

import io.netty.channel.ChannelHandlerContext;

import java.util.EventListener;

/**
 * @author kawasima
 */
public interface WebSocketMessageListener extends EventListener {
    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] message);
}
