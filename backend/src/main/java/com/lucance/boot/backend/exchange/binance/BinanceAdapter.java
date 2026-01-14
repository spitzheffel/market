package com.lucance.boot.backend.exchange.binance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lucance.boot.backend.config.ExchangeProperties;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.*;
import com.lucance.boot.backend.exchange.model.*;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Binance 交易所适配器
 * 参考: 01-phase1-api-integration.md
 */
@Service
public class BinanceAdapter extends BaseHttpClient implements ExchangeAdapter {

    private static final String EXCHANGE_NAME = "Binance";

    private BinanceWebSocketClient wsClient;
    private final ExchangeProperties properties;

    public BinanceAdapter(ExchangeProperties properties) {
        super(
                properties.getBinance().getBaseUrl(),
                properties.getBinance().getApiKey(),
                properties.getBinance().getSecretKey(),
                createProxyConfig(properties.getProxy()),
                properties.getBinance().getRateLimit().getRequestsPerSecond());
        this.properties = properties;
    }

    /**
     * 获取或创建 WebSocket 客户端（懒加载）
     */
    private synchronized BinanceWebSocketClient getWsClient() {
        if (wsClient == null) {
            wsClient = new BinanceWebSocketClient(createProxyConfig(properties.getProxy()));
            log.info("BinanceWebSocketClient initialized");
        }
        return wsClient;
    }

    private static ProxyConfig createProxyConfig(ExchangeProperties.ProxySettings proxy) {
        if (proxy == null || !proxy.isEnabled()) {
            return ProxyConfig.disabled();
        }

        return ProxyConfig.fromProperties(
                proxy.isEnabled(),
                proxy.getType(),
                proxy.getHost(),
                proxy.getPort(),
                proxy.getUsername(),
                proxy.getPassword());
    }

    @Override
    public String getExchangeName() {
        return EXCHANGE_NAME;
    }

    @Override
    public HealthStatus healthCheck() {
        try {
            long startTime = System.currentTimeMillis();
            get("/api/v3/ping", null, Map.class);
            long latency = System.currentTimeMillis() - startTime;
            return HealthStatus.healthy(EXCHANGE_NAME, latency);
        } catch (Exception e) {
            log.error("Health check failed", e);
            return HealthStatus.unhealthy(EXCHANGE_NAME, e.getMessage());
        }
    }

    @Override
    public List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime, int limit) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(symbol));
        params.put("interval", interval);

        if (startTime != null) {
            params.put("startTime", String.valueOf(startTime));
        }
        if (endTime != null) {
            params.put("endTime", String.valueOf(endTime));
        }
        int resolvedLimit = limit > 0 ? limit : 500;
        params.put("limit", String.valueOf(resolvedLimit));

        // Binance 返回二维数组格式
        List<List<Object>> response = get("/api/v3/klines", params, new TypeReference<>() {
        });

        return response.stream()
                .map(data -> convertToKline(symbol, interval, data))
                .toList();
    }

    @Override
    public Ticker getTicker(String symbol) {
        Map<String, String> params = Map.of("symbol", convertSymbol(symbol));
        Map<String, Object> response = get("/api/v3/ticker/24hr", params, new TypeReference<>() {
        });
        return convertToTicker(response);
    }

    @Override
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback) {
        String binanceSymbol = convertSymbol(symbol);
        getWsClient().subscribeKline(binanceSymbol, interval, callback);
        log.info("Subscribed to kline via adapter: {} {}", symbol, interval);
    }

    /**
     * 订阅只推送闭合K线
     */
    public void subscribeKlineClosedOnly(String symbol, String interval, Consumer<Kline> callback) {
        String binanceSymbol = convertSymbol(symbol);
        getWsClient().subscribeKline(binanceSymbol, interval, callback, true);
        log.info("Subscribed to closed kline via adapter: {} {}", symbol, interval);
    }

    @Override
    public void unsubscribeKline(String symbol, String interval) {
        String binanceSymbol = convertSymbol(symbol);
        getWsClient().unsubscribeKline(binanceSymbol, interval);
        log.info("Unsubscribed from kline via adapter: {} {}", symbol, interval);
    }

    @Override
    public Order placeOrder(OrderRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(request.getSymbol()));
        params.put("side", request.getSide().toUpperCase());
        params.put("type", request.getType().toUpperCase());
        params.put("quantity", request.getSize().toPlainString());

        if ("LIMIT".equalsIgnoreCase(request.getType())) {
            params.put("price", request.getPrice().toPlainString());
            params.put("timeInForce", request.getTimeInForce() != null ? request.getTimeInForce() : "GTC");
        }

        Map<String, Object> response = post("/api/v3/order", params, new TypeReference<>() {
        });
        return convertToOrder(response);
    }

    @Override
    public Order cancelOrder(String orderId, String symbol) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(symbol));
        params.put("orderId", orderId);

        Map<String, Object> response = delete("/api/v3/order", params, new TypeReference<>() {
        });
        return convertToOrder(response);
    }

    @Override
    public Order getOrder(String orderId, String symbol) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(symbol));
        params.put("orderId", orderId);
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String queryString = buildQueryString(params);
        params.put("signature", sign(queryString));

        Map<String, Object> response = get("/api/v3/order", params, new TypeReference<>() {
        }, true); // signed = true
        return convertToOrder(response);
    }

    @Override
    public List<Balance> getBalances() {
        Map<String, String> params = new HashMap<>();
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String queryString = buildQueryString(params);
        params.put("signature", sign(queryString));

        Map<String, Object> response = get("/api/v3/account", params, new TypeReference<>() {
        }, true); // signed = true

        @SuppressWarnings("unchecked")
        List<Map<String, String>> balances = (List<Map<String, String>>) response.get("balances");

        return balances.stream()
                .filter(b -> new BigDecimal(b.get("free")).compareTo(BigDecimal.ZERO) > 0 ||
                        new BigDecimal(b.get("locked")).compareTo(BigDecimal.ZERO) > 0)
                .map(b -> Balance.builder()
                        .asset(b.get("asset"))
                        .free(new BigDecimal(b.get("free")))
                        .locked(new BigDecimal(b.get("locked")))
                        .build())
                .toList();
    }

    @Override
    protected String sign(String payload) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new ExchangeApiException("Secret key is not configured");
        }

        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] hash = sha256HMAC.doFinal(payload.getBytes());
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new ExchangeApiException("Failed to sign request", e);
        }
    }

    /**
     * 转换交易对格式: BTC/USDT -> BTCUSDT
     */
    private String convertSymbol(String symbol) {
        return symbol.replace("/", "");
    }

    /**
     * 转换为统一的 Kline 格式
     * Binance 格式: [openTime, open, high, low, close, volume, closeTime, ...]
     */
    private Kline convertToKline(String symbol, String interval, List<Object> data) {
        return Kline.builder()
                .symbol(symbol)
                .interval(interval)
                .time(Instant.ofEpochMilli(((Number) data.get(0)).longValue()))
                .open(new BigDecimal(data.get(1).toString()))
                .high(new BigDecimal(data.get(2).toString()))
                .low(new BigDecimal(data.get(3).toString()))
                .close(new BigDecimal(data.get(4).toString()))
                .volume(new BigDecimal(data.get(5).toString()))
                .build();
    }

    private Ticker convertToTicker(Map<String, Object> response) {
        return Ticker.builder()
                .symbol(response.get("symbol").toString())
                .lastPrice(new BigDecimal(response.get("lastPrice").toString()))
                .high(new BigDecimal(response.get("highPrice").toString()))
                .low(new BigDecimal(response.get("lowPrice").toString()))
                .volume(new BigDecimal(response.get("volume").toString()))
                .priceChange(new BigDecimal(response.get("priceChange").toString()))
                .priceChangePercent(new BigDecimal(response.get("priceChangePercent").toString()))
                .timestamp(((Number) response.get("closeTime")).longValue())
                .build();
    }

    private Order convertToOrder(Map<String, Object> response) {
        return Order.builder()
                .id(response.get("orderId").toString())
                .exchangeOrderId(response.get("orderId").toString())
                .symbol(response.get("symbol").toString())
                .side(response.get("side").toString())
                .type(response.get("type").toString())
                .price(new BigDecimal(response.getOrDefault("price", "0").toString()))
                .size(new BigDecimal(response.get("origQty").toString()))
                .filledSize(new BigDecimal(response.getOrDefault("executedQty", "0").toString()))
                .status(convertOrderStatus(response.get("status").toString()))
                .createdAt(((Number) response.getOrDefault("transactTime", 0L)).longValue())
                .build();
    }

    private OrderStatus convertOrderStatus(String status) {
        return switch (status) {
            case "NEW" -> OrderStatus.OPEN;
            case "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED;
            case "FILLED" -> OrderStatus.FILLED;
            case "CANCELED" -> OrderStatus.CANCELLED;
            case "REJECTED" -> OrderStatus.REJECTED;
            case "EXPIRED" -> OrderStatus.EXPIRED;
            default -> OrderStatus.UNKNOWN;
        };
    }
}
