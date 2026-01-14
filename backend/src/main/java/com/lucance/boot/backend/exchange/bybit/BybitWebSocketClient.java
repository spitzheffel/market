package com.lucance.boot.backend.exchange.bybit;

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
 * Bybit WebSocket 客户端
 * API 文档: https://bybit-exchange.github.io/docs/v5/websocket/public/kline
 */
@Slf4j
public class BybitWebSocketClient extends BaseWebSocketClient {

    private static final String WS_BASE_URL = "wss://stream.bybit.com/v5/public/spot";

    // 存储回调函数
    private final Map<String, Consumer<Kline>> klineCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> closedOnlyFlags = new ConcurrentHashMap<>();

    public BybitWebSocketClient() {
        super();
    }

    public BybitWebSocketClient(ProxyConfig proxyConfig) {
        super(proxyConfig);
    }

    /**
     * 订阅 K线数据
     */
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback, boolean closedOnly) {
        String bybitSymbol = convertSymbol(symbol);
        String bybitInterval = convertInterval(interval);
        String topic = "kline." + bybitInterval + "." + bybitSymbol;

        klineCallbacks.put(topic, callback);
        closedOnlyFlags.put(topic, closedOnly);

        log.info("Subscribing to Bybit kline: {} (closedOnly={})", topic, closedOnly);

        if (!isConnected()) {
            connect(WS_BASE_URL);
        }

        sendSubscribeMessage(topic);
    }

    /**
     * 取消订阅 K线
     */
    public void unsubscribeKline(String symbol, String interval) {
        String bybitSymbol = convertSymbol(symbol);
        String bybitInterval = convertInterval(interval);
        String topic = "kline." + bybitInterval + "." + bybitSymbol;

        klineCallbacks.remove(topic);
        closedOnlyFlags.remove(topic);
        sendUnsubscribeMessage(topic);
        log.info("Unsubscribed from Bybit kline: {}", topic);
    }

    @Override
    protected void onConnected() {
        log.info("Bybit WebSocket connected, resubscribing {} channels", subscriptions.size());
        klineCallbacks.keySet().forEach(this::sendSubscribeMessage);
    }

    @Override
    protected void sendSubscribeMessage(String channel) {
        try {
            String subscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "op", "subscribe",
                    "args", new String[]{channel}));
            send(subscribeMsg);
            log.debug("Sent Bybit subscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create Bybit subscribe message", e);
        }
    }

    @Override
    protected void sendUnsubscribeMessage(String channel) {
        try {
            String unsubscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "op", "unsubscribe",
                    "args", new String[]{channel}));
            send(unsubscribeMsg);
            log.debug("Sent Bybit unsubscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create Bybit unsubscribe message", e);
        }
    }

    @Override
    protected void handleMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // 处理订阅确认
            if (json.has("op")) {
                String op = json.get("op").asText();
                if ("subscribe".equals(op)) {
                    log.debug("Bybit subscription confirmed");
                }
                return;
            }

            // 处理心跳
            if (json.has("op") && "pong".equals(json.get("op").asText())) {
                return;
            }

            // 处理 K线数据
            if (json.has("topic") && json.has("data")) {
                String topic = json.get("topic").asText();
                JsonNode dataArray = json.get("data");

                if (dataArray.isArray() && dataArray.size() > 0) {
                    for (JsonNode klineData : dataArray) {
                        // Bybit K线格式包含 confirm 字段
                        // confirm: true = 已闭合, false = 未闭合
                        boolean isClosed = klineData.has("confirm") && klineData.get("confirm").asBoolean();

                        Boolean closedOnly = closedOnlyFlags.get(topic);
                        if (closedOnly != null && closedOnly && !isClosed) {
                            // 仅闭合模式：跳过未闭合的K线
                            continue;
                        }

                        Kline kline = parseKlineMessage(topic, klineData);
                        Consumer<Kline> callback = klineCallbacks.get(topic);
                        if (callback != null) {
                            callback.accept(kline);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to handle Bybit message: {}", message, e);
        }
    }

    /**
     * 解析 K线消息
     * Bybit 格式: {start, end, interval, open, close, high, low, volume, turnover, confirm, timestamp}
     */
    private Kline parseKlineMessage(String topic, JsonNode data) {
        // 从 topic 提取 symbol 和 interval
        // topic 格式: "kline.1.BTCUSDT"
        String[] parts = topic.split("\\.");
        String interval = convertIntervalBack(parts[1]);
        String bybitSymbol = parts[2];

        // 转换为标准格式 (BTCUSDT -> BTC/USDT)
        String symbol = bybitSymbol;
        if (bybitSymbol.endsWith("USDT")) {
            symbol = bybitSymbol.replace("USDT", "/USDT");
        } else if (bybitSymbol.endsWith("USDC")) {
            symbol = bybitSymbol.replace("USDC", "/USDC");
        }

        return Kline.builder()
                .symbol(symbol)
                .interval(interval)
                .time(Instant.ofEpochMilli(data.get("start").asLong()))
                .open(new BigDecimal(data.get("open").asText()))
                .high(new BigDecimal(data.get("high").asText()))
                .low(new BigDecimal(data.get("low").asText()))
                .close(new BigDecimal(data.get("close").asText()))
                .volume(new BigDecimal(data.get("volume").asText()))
                .build();
    }

    /**
     * 转换交易对格式: BTC/USDT -> BTCUSDT
     */
    private String convertSymbol(String symbol) {
        return symbol.replace("/", "");
    }

    /**
     * 转换周期格式
     */
    private String convertInterval(String interval) {
        return switch (interval) {
            case "1m" -> "1";
            case "3m" -> "3";
            case "5m" -> "5";
            case "15m" -> "15";
            case "30m" -> "30";
            case "1h" -> "60";
            case "2h" -> "120";
            case "4h" -> "240";
            case "6h" -> "360";
            case "12h" -> "720";
            case "1d" -> "D";
            case "1w" -> "W";
            case "1M" -> "M";
            default -> interval;
        };
    }

    /**
     * 反向转换周期格式
     */
    private String convertIntervalBack(String interval) {
        return switch (interval) {
            case "1" -> "1m";
            case "3" -> "3m";
            case "5" -> "5m";
            case "15" -> "15m";
            case "30" -> "30m";
            case "60" -> "1h";
            case "120" -> "2h";
            case "240" -> "4h";
            case "360" -> "6h";
            case "720" -> "12h";
            case "D" -> "1d";
            case "W" -> "1w";
            case "M" -> "1M";
            default -> interval;
        };
    }
}
