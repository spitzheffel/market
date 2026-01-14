package com.lucance.boot.backend.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * WebSocket 客户端基类
 * 参考: 01-phase1-api-integration.md
 */
@Slf4j
public abstract class BaseWebSocketClient {

    protected final OkHttpClient client;
    protected WebSocket webSocket;
    protected final Map<String, Consumer<String>> subscriptions = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    protected final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean isConnected = false;
    private volatile boolean shouldReconnect = true;
    private String currentUrl;

    public BaseWebSocketClient() {
        this(null);
    }

    public BaseWebSocketClient(ProxyConfig proxyConfig) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS);

        // 配置代理
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            configureProxy(builder, proxyConfig);
        }

        this.client = builder.build();
    }

    /**
     * 配置代理
     */
    private void configureProxy(OkHttpClient.Builder builder, ProxyConfig config) {
        try {
            Proxy proxy = new Proxy(
                    config.getType(),
                    new InetSocketAddress(config.getHost(), config.getPort()));
            builder.proxy(proxy);

            if (config.getUsername() != null && config.getPassword() != null) {
                builder.proxyAuthenticator((route, response) -> {
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null;
                    }

                    String credential = Credentials.basic(
                            config.getUsername(),
                            config.getPassword());

                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }

            log.info("WebSocket proxy configured: {}://{}:{}",
                    config.getType(), config.getHost(), config.getPort());

        } catch (Exception e) {
            log.error("Failed to configure WebSocket proxy", e);
            throw new RuntimeException("Failed to configure WebSocket proxy", e);
        }
    }

    /**
     * 连接 WebSocket
     */
    public void connect(String url) {
        this.currentUrl = url;
        this.shouldReconnect = true;

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket connected: {}", url);
                isConnected = true;
                onConnected();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket error: {}", t.getMessage());
                isConnected = false;

                if (shouldReconnect) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket closed: {} {}", code, reason);
                isConnected = false;

                if (shouldReconnect) {
                    scheduleReconnect();
                }
            }
        });
    }

    /**
     * 发送消息
     */
    protected void send(String message) {
        if (webSocket != null && isConnected) {
            webSocket.send(message);
        } else {
            log.warn("Cannot send message, WebSocket not connected");
        }
    }

    /**
     * 添加订阅
     */
    protected void addSubscription(String channel, Consumer<String> callback) {
        subscriptions.put(channel, callback);

        if (isConnected) {
            sendSubscribeMessage(channel);
        }
    }

    /**
     * 移除订阅
     */
    protected void removeSubscription(String channel) {
        subscriptions.remove(channel);
        sendUnsubscribeMessage(channel);
    }

    /**
     * 关闭连接
     */
    public void close() {
        shouldReconnect = false;
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }
        scheduler.shutdown();
        subscriptions.clear();
    }

    /**
     * 重连调度
     */
    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            log.info("Attempting to reconnect...");
            connect(currentUrl);
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 连接成功后的处理（子类实现）
     */
    protected abstract void onConnected();

    /**
     * 发送订阅消息（子类实现）
     */
    protected abstract void sendSubscribeMessage(String channel);

    /**
     * 发送取消订阅消息（子类实现）
     */
    protected abstract void sendUnsubscribeMessage(String channel);

    /**
     * 处理接收到的消息（子类实现）
     */
    protected abstract void handleMessage(String message);
}
