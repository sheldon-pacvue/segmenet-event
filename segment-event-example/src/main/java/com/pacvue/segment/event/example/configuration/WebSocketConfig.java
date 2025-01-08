package com.pacvue.segment.event.example.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;


@Configuration
public class WebSocketConfig {
    @Bean
    public CommandLineRunner startTcpServer() {
        return args -> {
            // 启动 TCP 服务
            startTcpServerExample().block();  // 阻塞等待 TCP 服务完成，阻塞会在实际场景中被替换成更灵活的异步处理方式
        };
    }

    private Mono<Void> startTcpServerExample() {
        // 启动 Reactor Netty TCP 服务
        return TcpServer.create()
                .host("localhost")
                .port(9090)  // 设置监听端口
                .handle((in, out) -> {
                    // 读取客户端发送的消息并打印
                    in.receive()
                            .asString()
                            .doOnNext(message -> System.out.println("Received message: \n\r" + message))
                            .subscribe();

                    // 向客户端发送响应
                    out.sendString(Mono.just("Hello from WebFlux TCP Server"))
                            .then()
                            .subscribe();

                    return Mono.never();  // 维持连接不关闭
                })
                .bind()  // 启动并绑定服务
                .doOnTerminate(() -> System.out.println("TCP Server Terminated"))
                .then();
    }
}
