package com.pacvue.segment.event.springboot.properties.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class HttpProperties extends BufferProperties {
    // 连接池配置 =============================================
    private String threadName = "segment-event";
    /**
     * 最大连接数
     *
     * 设置连接池中可以同时维持的最大连接数。当连接数超过此限制时，新的连接请求会被阻塞，直到有空闲连接。
     */
    private int maxConnections = 100;

    /**
     * 最大空闲时间（秒）
     *
     * 指定连接在连接池中空闲的最长时间，超过此时间未被使用的连接将被关闭。
     */
    private int maxIdleTime = 30;

    /**
     * 最大生命周期（秒）
     *
     * 设置连接的最大生命周期，超过此生命周期的连接将被关闭并重新创建。
     */
    private int maxLifeTime = 60;

    /**
     * 最大并发请求数，表示可以等待获取连接的请求数量
     *
     * 当有超过 `pendingAcquireMaxCount` 的请求需要连接时，额外的请求将会被阻塞或被拒绝。
     */
    private int pendingAcquireMaxCount = 100;

    /**
     * 获取连接的最大等待时间（秒）
     *
     * 在没有可用连接的情况下，请求连接的最大等待时间。如果超过此时间还未能获得连接，将抛出异常。
     */
    private long pendingAcquireTimeout = 5;


    // 连接配置 =============================================

    /**
     * 基础URL，例如 https://api.segment.com
     *
     * 设置API的基础URL，HttpClient会将此URL作为请求的前缀。
     */
    private String baseUrl = "https://api.segment.io/";

    /**
     * 连接超时（毫秒）
     *
     * 指定建立连接时的最大等待时间，如果连接超时，抛出连接超时异常。
     */
    private int connectionTimeout = 5000;

    /**
     * 请求超时（秒）
     *
     * 设置请求期间等待响应的最大时间。如果超时，抛出请求超时异常。
     */
    private int responseTimeout = 60;

    /**
     * 读取超时（秒）
     *
     * 设置请求时读取响应数据的最大超时时间。若响应数据超过此时间未能返回，将抛出超时异常。
     */
    private int readTimeout = 10;

    /**
     * 写入超时（秒）
     *
     * 设置请求时写入数据的最大超时时间。如果写入数据超过此时间没有完成，则抛出超时异常。
     */
    private int writeTimeout = 10;

    // 请求配置 =============================================
    /**
     * 请求资源路径
     */
    private String uri = "/v1/import";

    /**
     * 请求方式， POST/GET/OPTIONS/PUT/DELETE/PATCH
     */
    private String method = "POST";

    /**
     * 重试次数
     *
     * 设置在请求失败时的最大重试次数。可以通过`HttpClient`的重试机制进行配置，增强容错性。
     */
    private int retry = 3;
    /**
     * 授权信息
     */
    private String authorization;
}
