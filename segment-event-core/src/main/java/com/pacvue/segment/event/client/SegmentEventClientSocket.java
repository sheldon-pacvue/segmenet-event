package com.pacvue.segment.event.client;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.socket.SocketUtil;
import com.segment.analytics.messages.Message;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SegmentEventClientSocket<T extends Message> extends AbstractBufferSegmentEventClient<T, SegmentEventClientSocket<T>> {
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final static int SOCKET_TIMEOUT = 5;
    // 常量定义
    private static final String DEFAULT_LIB_NAME = "defaultLibrary";
    private static final String DEFAULT_LIB_VERSION = "0.0.0";
    private final String host;
    private final int port;
    private final String secret;
    private final String endPoint;

    private volatile Socket socket;
    private long lastActivityTime;  // 上次活动时间

    SegmentEventClientSocket(@NonNull String host, int port, @NotNull String secret, @NonNull String endPoint) {
        this.host = host;
        this.port = port;
        this.secret = secret;
        this.endPoint = endPoint;
        startTimeoutChecker();
    }


    @Override
    public Mono<Boolean> send(List<T> events) {
        // 向服务端发送消息
        return Mono.fromCallable(() -> {
                    lastActivityTime = System.currentTimeMillis();
                    if (null == socket) {
                        synchronized (this) {
                            if (null == socket) {
                                socket = SocketUtil.connect(host, port, 5000);
                            }
                        }
                    }
                    IoUtil.writeUtf8(socket.getOutputStream(), false, createMessage(events));
                    return true;
                });
    }

    @Override
    public void flush() {

    }

    // 启动超时检查定时任务
    private void startTimeoutChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            if (null == socket) {
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastActivityTime > TimeUnit.MINUTES.toMillis(SOCKET_TIMEOUT)) {
                // 超过 5 分钟没有活动，断开连接并通知服务端
                log.info("No activity for " + SOCKET_TIMEOUT + " minutes. Disconnecting and notifying server.");
                IoUtil.close(socket);  // 关闭连接
                socket = null;
            }
        }, 0, 1, TimeUnit.MINUTES);  // 每分钟检查一次
    }

    protected String createMessage(List<T> events) {
        // 验证事件列表和内容
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list is empty or null");
        }

        String content = gson.toJson(events);
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("Content is empty or null");
        }

        // 获取 library 信息
        String[] libraryInfo = extractLibraryInfo(events);
        String libName = libraryInfo[0];
        String libVersion = libraryInfo[1];

        // 构建请求头
        String authorization = Base64.getEncoder().encodeToString((this.secret + ":").getBytes(StandardCharsets.UTF_8));

        return HttpMethod.POST + StrUtil.SPACE + endPoint + StrUtil.SPACE + HttpVersion.HTTP_1_1 + "\r\n" +
                HttpHeaderNames.HOST + ": " + host + "\r\n" +
                HttpHeaderNames.CONTENT_TYPE + ": " + HttpHeaderValues.APPLICATION_JSON + "\r\n" +
                HttpHeaderNames.AUTHORIZATION + ": Basic " + authorization + "\r\n" +
                HttpHeaderNames.ACCEPT + ": " + HttpHeaderValues.APPLICATION_JSON + "\r\n" +
                HttpHeaderNames.USER_AGENT + ": " + libName + "/" + libVersion + "\r\n" +
                HttpHeaderNames.CONTENT_LENGTH + ": " + content.length() + "\r\n" +
                "\r\n" +
                content;
    }

    /**
     * 从事件列表中提取 library 的名称和版本信息。
     */
    private String[] extractLibraryInfo(List<T> events) {
        String libName = DEFAULT_LIB_NAME;
        String libVersion = DEFAULT_LIB_VERSION;

        try {
            Map<String, ?> context = events.get(0).context();
            if (context != null) {
                Map<String, Object> library = (Map<String, Object>) context.get("library");
                if (library != null) {
                    libName = (String) library.getOrDefault("name", DEFAULT_LIB_NAME);
                    libVersion = (String) library.getOrDefault("version", DEFAULT_LIB_VERSION);
                }
            }
        } catch (Exception ignored) {
        }

        return new String[]{libName, libVersion};
    }

    public static <T extends Message> SegmentEventClientSocket.Builder<T> builder() {
        return new SegmentEventClientSocket.Builder<>();
    }

    public static class Builder<T extends Message> {
        private String host;
        private int port;
        private String secret;
        private String endPoint;

        public Builder<T> host(String host) {
            this.host = host;
            return this;
        }

        public Builder<T> port(int port) {
            this.port = port;
            return this;
        }

        public Builder<T> secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder<T> endPoint(String endPoint) {
            this.endPoint = endPoint;
            return this;
        }

        public SegmentEventClientSocket<T> build() {
            return new SegmentEventClientSocket<>(host, port, secret, endPoint);
        }
    }
}
