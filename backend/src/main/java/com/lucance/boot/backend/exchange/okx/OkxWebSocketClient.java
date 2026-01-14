package com.lucance.boot.backend.exchange.okx;

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
 * OKX WebSocket 客户端
 * API 文档: https://www.okx.com/docs-v5/en/#websocket-api
 */
@Slf4j
public class OkxWebSocketClient extends BaseWebSocketClient {

    private static final String WS_BASE_URL = "wss://ws.okx.com:8443/ws/v5/public";

    // 存储回调函数
    private final Map<String, Consumer<Kline>> klineCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> closedOnlyFlags = new ConcurrentHashMap<>();

    public OkxWebSocketClient() {
        super();
    }

    public OkxWebSocketClient(ProxyConfig proxyConfig) {
        super(proxyConfig);
    }

    /**
     * 订阅 K线数据
     */
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback, boolean closedOnly) {
        String instId = convertSymbol(symbol);
        String channel = "candle" + convertInterval(interval);
        String stream = channel + ":" + instId;

        klineCallbacks.put(stream, callback);
        closedOnlyFlags.put(stream, closedOnly);

        log.info("Subscribing to OKX kline: {} (closedOnly={})", stream, closedOnly);

        if (!isConnected()) {
            connect(WS_BASE_URL);
        }

        sendSubscribeMessage(stream);
    }

    /**
     * 取消订阅 K线
     */
    public void unsubscribeKline(String symbol, String interval) {
        String instId = convertSymbol(symbol);
        String channel = "candle" + convertInterval(interval);
        String stream = channel + ":" + instId;

        klineCallbacks.remove(stream);
        closedOnlyFlags.remove(stream);
        sendUnsubscribeMessage(stream);
        log.info("Unsubscribed from OKX kline: {}", stream);
    }

    @Override
    protected void onConnected() {
        log.info("OKX WebSocket connected, resubscribing {} channels", subscriptions.size());
        klineCallbacks.keySet().forEach(this::sendSubscribeMessage);
    }

    @Override
    protected void sendSubscribeMessage(String channel) {
        try {
            String[] parts = channel.split(":");
            String channelName = parts[0];
            String instId = parts[1];

            String subscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "op", "subscribe",
                    "args", new Object[]{
                            Map.of("channel", channelName, "instId", instId)
                    }));
            send(subscribeMsg);
            log.debug("Sent OKX subscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create OKX subscribe message", e);
        }
    }

    @Override
    protected void sendUnsubscribeMessage(String channel) {
        try {
            String[] parts = channel.split(":");
            String channelName = parts[0];
            String instId = parts[1];

            String unsubscribeMsg = objectMapper.writeValueAsString(Map.of(
                    "op", "unsubscribe",
                    "args", new Object[]{
                            Map.of("channel", channelName, "instId", instId)
                    }));
            send(unsubscribeMsg);
            log.debug("Sent OKX unsubscribe message: {}", channel);
        } catch (Exception e) {
            log.error("Failed to create OKX unsubscribe message", e);
        }
    }

    @Override
    protected void handleMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // 处理订阅确认
            if (json.has("event")) {
                String event = json.get("event").asText();
                if ("subscribe".equals(event)) {
                    log.debug("OKX subscription confirmed");
                } else if ("error".equals(event)) {
                    log.error("OKX WebSocket error: {}", json.get("msg"));
                }
                return;
            }

            // 处理 K线数据
            if (json.has("arg") && json.has("data")) {
                JsonNode arg = json.get("arg");
                String channel = arg.get("channel").asText();
                String instId = arg.get("instId").asText();
                String stream = channel + ":" + instId;

                JsonNode dataArray = json.get("data");
                if (dataArray.isArray() && dataArray.size() > 0) {
                    JsonNode klineData = dataArray.get(0);

                    // OKX K线格式: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
                    // confirm: "0" = 未闭合, "1" = 已闭合
                    String confirm = klineData.get(8).asText();
                    boolean isClosed = "1".equals(confirm);

                    Boolean closedOnly = closedOnlyFlags.get(stream);
                    if (closedOnly != null && closedOnly && !isClosed) {
                        // 仅闭合模式：跳过未闭合的K线
                        return;
                    }

                    Kline kline = parseKlineMessage(instId, channel, klineData);
                    Consumer<Kline> callback = klineCallbacks.get(stream);
                    if (callback != null) {
                        callback.accept(kline);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to handle OKX message: {}", message, e);
        }
    }

    /**
     * 解析 K线消息
     * OKX 格式: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
     */
    private Kline parseKlineMessage(String instId, String channel, JsonNode data) {
        // 从 channel 提取 interval (如 "candle1m" -> "1m")
        String interval = channel.replace("candle", "");
        interval = convertIntervalBack(interval);

        // 转换 instId 为标准格式 (BTC-USDT -> BTC/USDT)
        String symbol = instId.replace("-", "/");

        return Kline.builder()
                .symbol(symbol)
                .interval(interval)
                .time(Instant.ofEpochMilli(data.get(0).asLong()))
                .open(new BigDecimal(data.get(1).asText()))
                .high(new BigDecimal(data.get(2).asText()))
                .low(new BigDecimal(data.get(3).asText()))
                .close(new BigDecimal(data.get(4).asText()))
                .volume(new BigDecimal(data.get(5).asText()))
                .build();
    }

    /**
     * 转换交易对格式: BTC/USDT -> BTC-USDT
     */
    private String convertSymbol(String symbol) {
        return symbol.replace("/", "-");
    }

    /**
     * 转换周期格式
     */
    private String convertInterval(String interval) {
        return switch (interval) {
            case "1m" -> "1m";
            case "5m" -> "5m";
            case "15m" -> "15m";
            case "30m" -> "30m";
            case "1h" -> "1H";
            case "4h" -> "4H";
            case "1d" -> "1D";
            case "1w" -> "1W";
            default -> interval;
        };
    }

    /**
     * 反向转换周期格式
     */
    private String convertIntervalBack(String interval) {
        return switch (interval) {
            case "1m" -> "1m";
            case "5m" -> "5m";
            case "15m" -> "15m";
            case "30m" -> "30m";
            case "1H" -> "1h";
            case "4H" -> "4h";
            case "1D" -> "1d";
            case "1W" -> "1w";
            default -> interval;
        };
    }
}
