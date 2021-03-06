import io.netty.channel.ChannelHandlerContext;
import net.unit8.uloncolon.WebSocketClient;
import net.unit8.uloncolon.WebSocketMessageListener;
import org.junit.Test;

/**
 * @author kawasima
 */
public class WebSocketClientTest {
    @Test
    public void test() throws InterruptedException {
        WebSocketClient client = new WebSocketClient(new WebSocketMessageListener() {
            @Override
            public void onBinaryMessage(ChannelHandlerContext ctx, byte[] message) {
                System.out.println(new String(message));
            }
        });
        client.connect("ws://localhost:5629");
        Thread.sleep(30 * 1000);
        client.close();
    }
}
