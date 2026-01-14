package com.lucance.boot.backend.exchange;

import lombok.Builder;
import lombok.Data;

import java.net.Proxy;

/**
 * 代理配置
 * 参考: 01-phase1-api-integration.md
 */
@Data
@Builder
public class ProxyConfig {

    /**
     * 是否启用代理
     */
    private boolean enabled;

    /**
     * 代理类型
     */
    private Proxy.Type type; // HTTP, SOCKS

    /**
     * 代理主机
     */
    private String host;

    /**
     * 代理端口
     */
    private int port;

    /**
     * 代理用户名（可选）
     */
    private String username;

    /**
     * 代理密码（可选）
     */
    private String password;

    /**
     * 从配置属性创建
     */
    public static ProxyConfig fromProperties(
            boolean enabled,
            String type,
            String host,
            Integer port,
            String username,
            String password) {

        if (!enabled) {
            return ProxyConfig.builder().enabled(false).build();
        }

        Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(type)
                ? Proxy.Type.SOCKS
                : Proxy.Type.HTTP;

        return ProxyConfig.builder()
                .enabled(true)
                .type(proxyType)
                .host(host)
                .port(port != null ? port : 1080)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * 禁用代理的配置
     */
    public static ProxyConfig disabled() {
        return ProxyConfig.builder().enabled(false).build();
    }
}
