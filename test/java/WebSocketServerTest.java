import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import net.unit8.uloncolon.WebSocketServer;
import net.unit8.uloncolon.WebSocketServerHandler;
import net.unit8.uloncolon.WebSocketServerHandlerFactory;
import org.junit.Test;

/**
 * @author kawasima
 */
public class WebSocketServerTest {
    @Test
    public void test() {
        WebSocketServer server = new WebSocketServer(new WebSocketServerHandlerFactory() {
            @Override
            public WebSocketServerHandler create() {
                return new WebSocketServerHandler() {
                    @Override
                    public void onConnect(ChannelHandlerContext ctx) {
                        ByteBuf buf = Unpooled.copiedBuffer("hoge".getBytes());
                        ctx.writeAndFlush(new BinaryWebSocketFrame(buf));
                    }

                    @Override
                    public void onDisconnect(ChannelHandlerContext ctx) {

                    }

                    @Override
                    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] message) {
                        System.out.println("server ni message" + message);
                    }
                };
            }
        }, 5629);
        server.close();
    }
}
