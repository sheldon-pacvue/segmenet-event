
import cn.hutool.core.io.IoUtil;
import cn.hutool.socket.SocketUtil;
import com.pacvue.segementeventexample.SegmentEventExampleApplication;
import com.pacvue.segment.event.client.SegmentEventClientSocket;
import com.pacvue.segment.event.core.SegmentEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

@SpringBootTest(classes = SegmentEventExampleApplication.class)
public class SocketTest {
    @Test
    public void test() {
        SegmentEventClientSocket segmentEventClientSocket = new SegmentEventClientSocket("localhost", 9090, "123", "/ws/echo");
        segmentEventClientSocket.send(List.of(new SegmentEvent()))
                .block();
    }

    @Test
    public void test2() throws IOException {
        Socket socket = SocketUtil.connect("localhost", 9090, 10000);
        IoUtil.writeUtf8(socket.getOutputStream(), false, "hello world");
    }
}
