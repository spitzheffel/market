package com.lucance.boot.backend.exchange.bybit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lucance.boot.backend.config.ExchangeProperties;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.BaseHttpClient;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.exchange.ExchangeApiException;
import com.lucance.boot.backend.exchange.ProxyConfig;
import com.lucance.boot.backend.exchange.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Bybit 交易所适配器 (V5 API)
 * API 文档: https://bybit-exchange.github.io/docs/v5/intro
 */
@Slf4j
public class BybitAdapter extends BaseHttpClient implements ExchangeAdapter {

    private static final String EXCHANGE_NAME = "BYBIT";

    private BybitWebSocketClient wsClient;
    private final ProxyConfig proxyConfig;

    public BybitAdapter(ExchangeProperties.BybitConfig config, ProxyConfig proxyConfig) {
        super(config.getBaseUrl(), config.getApiKey(), config.getSecretKey(), proxyConfig, 10.0);
        this.proxyConfig = proxyConfig;
        log.info("BybitAdapter initialized: baseUrl={}", config.getBaseUrl());
    }

    /**
     * 获取或创建 WebSocket 客户端（懒加载）
     */
    private synchronized BybitWebSocketClient getWsClient() {
        if (wsClient == null) {
            wsClient = new BybitWebSocketClient(proxyConfig);
            log.info("BybitWebSocketClient initialized");
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
            Map<String, Object> response = get("/v5/market/time", params, new TypeReference<>() {
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
            params.put("category", "spot");
            params.put("symbol", convertSymbol(symbol));
            params.put("interval", convertInterval(interval));
            params.put("limit", String.valueOf(Math.min(limit, 1000)));

            if (startTime != null) {
                params.put("start", String.valueOf(startTime));
            }
            if (endTime != null) {
                params.put("end", String.valueOf(endTime));
            }

            Map<String, Object> response = get("/v5/market/kline", params, new TypeReference<>() {
            });

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");

            if (result == null) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<List<String>> data = (List<List<String>>) result.get("list");

            if (data == null || data.isEmpty()) {
                return new ArrayList<>();
            }

            return data.stream()
                    .map(item -> convertToKline(symbol, interval, item))
                    .sorted(Comparator.comparing(Kline::getTime))
                    .toList();

        } catch (Exception e) {
            throw new ExchangeApiException("Failed to get klines from Bybit", e);
        }
    }

    @Override
    public Ticker getTicker(String symbol) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("category", "spot");
            params.put("symbol", convertSymbol(symbol));

            Map<String, Object> response = get("/v5/market/tickers", params, new TypeReference<>() {
            });

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> list = (List<Map<String, String>>) result.get("list");

            if (list == null || list.isEmpty()) {
                throw new ExchangeApiException("No ticker data");
            }

            return convertToTicker(list.get(0));
        } catch (Exception e) {
            throw new ExchangeApiException("Failed to get ticker from Bybit", e);
        }
    }

    @Override
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback) {
        getWsClient().subscribeKline(symbol, interval, callback, false);
        log.info("Subscribed to Bybit kline: {} {}", symbol, interval);
    }

    @Override
    public void unsubscribeKline(String symbol, String interval) {
        getWsClient().unsubscribeKline(symbol, interval);
        log.info("Unsubscribed from Bybit kline: {} {}", symbol, interval);
    }

    @Override
    public Order placeOrder(OrderRequest request) {
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
            throw new ExchangeApiException("Secret key is not configured for Bybit");
        }

        // Bybit V5 使用 HMAC-SHA256
        return HmacUtils.hmacSha256Hex(secretKey, payload);
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
     * Bybit K线格式: [startTime, openPrice, highPrice, lowPrice, closePrice, volume,
     * turnover]
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
                .symbol(data.get("symbol"))
                .lastPrice(new BigDecimal(data.get("lastPrice")))
                .high(new BigDecimal(data.get("highPrice24h")))
                .low(new BigDecimal(data.get("lowPrice24h")))
                .volume(new BigDecimal(data.get("volume24h")))
                .priceChange(new BigDecimal(data.getOrDefault("price24hPcnt", "0")))
                .priceChangePercent(
                        new BigDecimal(data.getOrDefault("price24hPcnt", "0")).multiply(new BigDecimal("100")))
                .build();
    }
}
