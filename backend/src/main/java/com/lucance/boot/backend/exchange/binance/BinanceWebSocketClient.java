package com.lucance.boot.backend.exchange.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.BaseWebSocketClient;
import com.lucance.boot.backend.exchange.ProxyConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Binance WebSocket 客户端
 * 支持闭合 K 线过滤
 */
@Slf4j
public class BinanceWebSocketClient extends BaseWebSocketClient {

    private static final String WS_BASE_URL = "wss://stream.binance.com:9443";

    // 存储回调函数
    private final Map<String, Consumer<Kline>> klineCallbacks = new ConcurrentHashMap<>();

    // 存储闭合K线回调（仅闭合时触发）
    private final Map<String, Consumer<Kline>> closedKlineCallbacks = new ConcurrentHashMap<>();

    // 是否只通知闭合K线
    private final Map<String, Boolean> closedOnlyFlags = new ConcurrentHashMap<>();

    public BinanceWebSocketClient() {
        super();
    }

    public BinanceWebSocketClient(ProxyConfig proxyConfig) {
        super(proxyConfig);
    }

    /**
     * 订阅 K线数据
     * 
     * @param symbol   交易对 (如 "BTCUSDT")
     * @param interval 周期 (如 "1m")
     * @param callback 回调函数
     */
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback) {
        subscribeKline(symbol, interval, callback, false);
    }

    /**
     * 订阅 K线数据（支持闭合过滤）
     * 
     * @param symbol     交易对 (如 "BTCUSDT")
     * @param interval   周期 (如 "1m")
     * @param callback   回调函数
     * @param closedOnly 是否仅闭合K线
     */
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback, boolean closedOnly) {
        String stream = symbol.toLowerCase() + "@kline_" + interval;

        if (closedOnly) {
            closedKlineCallbacks.put(stream, callback);
            closedOnlyFlags.put(stream, true);
        } else {
            klineCallbacks.put(stream, callback);
            closedOnlyFlags.put(stream, false);
        }

        // 使用组合流URL
        String url = WS_BASE_URL + "/ws/" + stream;
        log.info("Subscribing to kline: {} (closedOnly={})", stream, closedOnly);

        if (!isConnected()) {
            connect(url);
        } else {
            addSubscription(stream, msg -> {
            });
        }
    }

    /**
     * 取消订阅 K线
     */
    public void unsubscribeKline(String symbol, String interval) {
        String stream = symbol.toLowerCase() + "@kline_" + interval;
        klineCallbacks.remove(stream);
        closedKlineCallbacks.remove(stream);
        closedOnlyFlags.remove(stream);
        removeSubscription(stream);
        log.info("Unsubscribed from kline: {}", stream);
    }

    @Override
    protected void onConnected() {
        log.info("WebSocket connected, resubscribing {} channels", subscriptions.size());
        subscriptions.keySet().forEach(this::sendSubscribeMessage);
    }

    @Override
    protected void sendSubscribeMessage(String channel) {
        try {
            String subscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "method", "SUBSCRIBE",
                    "params", new String[] { channel },
                    "id", System.currentTimeMillis()));
            send(subscribeMsg);
            log.debug("Sent subscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create subscribe message", e);
        }
    }

    @Override
    protected void sendUnsubscribeMessage(String channel) {
        try {
            String unsubscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "method", "UNSUBSCRIBE",
                    "params", new String[] { channel },
                    "id", System.currentTimeMillis()));
            send(unsubscribeMsg);
            log.debug("Sent unsubscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create unsubscribe message", e);
        }
    }

    @Override
    protected void handleMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // 处理 K线数据
            if (json.has("e") && "kline".equals(json.get("e").asText())) {
                JsonNode k = json.get("k");
                boolean isClosed = k.get("x").asBoolean();

                String stream = json.get("s").asText().toLowerCase() + "@kline_" +
                        k.get("i").asText();

                // 检查是否需要只处理闭合K线
                Boolean closedOnly = closedOnlyFlags.get(stream);

                if (closedOnly != null && closedOnly) {
                    // 仅闭合模式：只有闭合时才回调
                    if (isClosed) {
                        Kline kline = parseKlineMessage(json, true);
                        Consumer<Kline> callback = closedKlineCallbacks.get(stream);
                        if (callback != null) {
                            log.debug("Closed kline received: {} {}", kline.getSymbol(), kline.getInterval());
                            callback.accept(kline);
                        }
                    }
                } else {
                    // 实时模式：每次更新都回调
                    Kline kline = parseKlineMessage(json, isClosed);
                    Consumer<Kline> callback = klineCallbacks.get(stream);
                    if (callback != null) {
                        callback.accept(kline);
                    }
                }
            }
            // 处理订阅确认
            else if (json.has("result") && json.get("result").isNull()) {
                log.debug("Subscription confirmed");
            }
            // 处理错误
            else if (json.has("error")) {
                log.error("WebSocket error: {}", json.get("error"));
            }

        } catch (Exception e) {
            log.error("Failed to handle message: {}", message, e);
        }
    }

    /**
     * 解析 K线消息
     * 格式: {"e":"kline","E":1234567890,"s":"BTCUSDT","k":{...,"x":true/false}}
     * 
     * @param json     K线 JSON 数据
     * @param isClosed 是否已闭合
     */
    private Kline parseKlineMessage(JsonNode json, boolean isClosed) {
        JsonNode k = json.get("k");
        String symbol = json.get("s").asText();

        // 转换为标准格式 BTCUSDT -> BTC/USDT
        String standardSymbol = symbol;
        if (symbol.endsWith("USDT")) {
            standardSymbol = symbol.replace("USDT", "/USDT");
        } else if (symbol.endsWith("BUSD")) {
            standardSymbol = symbol.replace("BUSD", "/BUSD");
        }

        return Kline.builder()
                .symbol(standardSymbol)
                .interval(k.get("i").asText())
                .time(Instant.ofEpochMilli(k.get("t").asLong()))
                .open(new BigDecimal(k.get("o").asText()))
                .high(new BigDecimal(k.get("h").asText()))
                .low(new BigDecimal(k.get("l").asText()))
                .close(new BigDecimal(k.get("c").asText()))
                .volume(new BigDecimal(k.get("v").asText()))
                .build();
    }

    /**
     * 检查 K 线是否闭合
     */
    public static boolean isKlineClosed(JsonNode klineJson) {
        return klineJson.has("x") && klineJson.get("x").asBoolean();
    }
}
