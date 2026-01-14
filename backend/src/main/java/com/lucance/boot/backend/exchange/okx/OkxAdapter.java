package com.lucance.boot.backend.exchange.okx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lucance.boot.backend.config.ExchangeProperties;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.BaseHttpClient;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.exchange.ExchangeApiException;
import com.lucance.boot.backend.exchange.ProxyConfig;
import com.lucance.boot.backend.exchange.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * OKX 交易所适配器
 * API 文档: https://www.okx.com/docs-v5/
 */
@Slf4j
public class OkxAdapter extends BaseHttpClient implements ExchangeAdapter {

    private static final String EXCHANGE_NAME = "OKX";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private final String passphrase;
    private OkxWebSocketClient wsClient;
    private final ProxyConfig proxyConfig;

    public OkxAdapter(ExchangeProperties.OkxConfig config, ProxyConfig proxyConfig) {
        super(config.getBaseUrl(), config.getApiKey(), config.getSecretKey(), proxyConfig, 10.0);
        this.passphrase = config.getPassphrase();
        this.proxyConfig = proxyConfig;
        log.info("OkxAdapter initialized: baseUrl={}", config.getBaseUrl());
    }

    /**
     * 获取或创建 WebSocket 客户端（懒加载）
     */
    private synchronized OkxWebSocketClient getWsClient() {
        if (wsClient == null) {
            wsClient = new OkxWebSocketClient(proxyConfig);
            log.info("OkxWebSocketClient initialized");
        }
        return wsClient;
    }

    @Override
    public String getExchangeName() {
        return EXCHANGE_NAME;
    }

    @Override
    public HealthStatus healthCheck() {
        long start = System.currentTimeMillis();
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, Object> response = get("/api/v5/public/time", params, new TypeReference<>() {
            });

            long latency = System.currentTimeMillis() - start;
            return HealthStatus.builder()
                    .healthy(true)
                    .exchange(EXCHANGE_NAME)
                    .latency(latency)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.warn("Health check failed", e);
            return HealthStatus.builder()
                    .healthy(false)
                    .exchange(EXCHANGE_NAME)
                    .message(e.getMessage())
                    .latency(-1)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Override
    public List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime, int limit) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("instId", convertSymbol(symbol));
            params.put("bar", convertInterval(interval));
            params.put("limit", String.valueOf(Math.min(limit, 300))); // OKX 最大300

            if (endTime != null) {
                params.put("before", String.valueOf(endTime));
            }
            if (startTime != null) {
                params.put("after", String.valueOf(startTime));
            }

            Map<String, Object> response = get("/api/v5/market/candles", params, new TypeReference<>() {
            });

            @SuppressWarnings("unchecked")
            List<List<String>> data = (List<List<String>>) response.get("data");

            if (data == null || data.isEmpty()) {
                return new ArrayList<>();
            }

            return data.stream()
                    .map(item -> convertToKline(symbol, interval, item))
                    .sorted(Comparator.comparing(Kline::getTime))
                    .toList();

        } catch (Exception e) {
            throw new ExchangeApiException("Failed to get klines from OKX", e);
        }
    }

    @Override
    public Ticker getTicker(String symbol) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("instId", convertSymbol(symbol));

            Map<String, Object> response = get("/api/v5/market/ticker", params, new TypeReference<>() {
            });

            @SuppressWarnings("unchecked")
            List<Map<String, String>> data = (List<Map<String, String>>) response.get("data");

            if (data == null || data.isEmpty()) {
                throw new ExchangeApiException("No ticker data");
            }

            return convertToTicker(data.get(0));
        } catch (Exception e) {
            throw new ExchangeApiException("Failed to get ticker from OKX", e);
        }
    }

    @Override
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback) {
        getWsClient().subscribeKline(symbol, interval, callback, false);
        log.info("Subscribed to OKX kline: {} {}", symbol, interval);
    }

    @Override
    public void unsubscribeKline(String symbol, String interval) {
        getWsClient().unsubscribeKline(symbol, interval);
        log.info("Unsubscribed from OKX kline: {} {}", symbol, interval);
    }

    @Override
    public Order placeOrder(OrderRequest request) {
        // 需要签名的接口
        throw new UnsupportedOperationException("Order placement not implemented yet");
    }

    @Override
    public Order cancelOrder(String orderId, String symbol) {
        throw new UnsupportedOperationException("Order cancellation not implemented yet");
    }

    @Override
    public Order getOrder(String orderId, String symbol) {
        throw new UnsupportedOperationException("Order query not implemented yet");
    }

    @Override
    public List<Balance> getBalances() {
        throw new UnsupportedOperationException("Balance query not implemented yet");
    }

    @Override
    protected String sign(String payload) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new ExchangeApiException("Secret key is not configured for OKX");
        }

        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] hash = sha256HMAC.doFinal(payload.getBytes());
            return Base64.encodeBase64String(hash);
        } catch (Exception e) {
            throw new ExchangeApiException("Failed to sign OKX request", e);
        }
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
     * OKX K线格式: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
     */
    private Kline convertToKline(String symbol, String interval, List<String> data) {
        return Kline.builder()
                .symbol(symbol)
                .interval(interval)
                .time(Instant.ofEpochMilli(Long.parseLong(data.get(0))))
                .open(new BigDecimal(data.get(1)))
                .high(new BigDecimal(data.get(2)))
                .low(new BigDecimal(data.get(3)))
                .close(new BigDecimal(data.get(4)))
                .volume(new BigDecimal(data.get(5)))
                .build();
    }

    private Ticker convertToTicker(Map<String, String> data) {
        return Ticker.builder()
                .symbol(data.get("instId"))
                .lastPrice(new BigDecimal(data.get("last")))
                .high(new BigDecimal(data.get("high24h")))
                .low(new BigDecimal(data.get("low24h")))
                .volume(new BigDecimal(data.get("vol24h")))
                .priceChange(new BigDecimal("0")) // OKX 需要计算
                .priceChangePercent(new BigDecimal("0"))
                .build();
    }
}
