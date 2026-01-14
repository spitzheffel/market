package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.repository.KlineRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 实时数据同步服务
 * 通过 ExchangeRouterService 统一管理订阅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeSyncService {

    private final KlineRepository klineRepository;
    private final ExchangeRouterService exchangeRouterService;

    private final Set<String> activeSubscriptions = ConcurrentHashMap.newKeySet();
    private final Map<String, Consumer<Kline>> externalCallbacks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("RealtimeSyncService initialized");
    }

    @PreDestroy
    public void destroy() {
        // 取消所有订阅
        activeSubscriptions.forEach(key -> {
            String[] parts = key.split("_");
            if (parts.length >= 3) {
                String exchange = parts[0];
                String symbol = parts[1];
                String interval = parts[2];
                try {
                    ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
                    adapter.unsubscribeKline(symbol, interval);
                } catch (Exception e) {
                    log.warn("Failed to unsubscribe: {}", key, e);
                }
            }
        });
        activeSubscriptions.clear();
        log.info("RealtimeSyncService destroyed");
    }

    /**
     * 订阅 K线数据
     *
     * @param exchange 交易所名称
     * @param symbol   交易对
     * @param interval 周期
     * @param saveToDb 是否保存到数据库（仅保存闭合K线）
     * @param callback 外部回调（可选）
     */
    public void subscribe(String exchange, String symbol, String interval,
            boolean saveToDb, Consumer<Kline> callback) {
        String key = exchange + "_" + symbol + "_" + interval;

        if (activeSubscriptions.contains(key)) {
            log.warn("Already subscribed to: {}", key);
            return;
        }

        if (callback != null) {
            externalCallbacks.put(key, callback);
        }

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);

        // 如果需要保存到数据库，使用闭合K线订阅
        if (saveToDb) {
            // 对于 Binance，使用 closedOnly 模式
            if ("binance".equalsIgnoreCase(exchange) && adapter instanceof com.lucance.boot.backend.exchange.binance.BinanceAdapter) {
                com.lucance.boot.backend.exchange.binance.BinanceAdapter binanceAdapter =
                    (com.lucance.boot.backend.exchange.binance.BinanceAdapter) adapter;
                binanceAdapter.subscribeKlineClosedOnly(symbol, interval, kline -> {
                    log.debug("Received closed kline: {} {} close={}", kline.getSymbol(), kline.getInterval(), kline.getClose());

                    // 保存到数据库
                    try {
                        klineRepository.save(kline);
                    } catch (Exception e) {
                        log.error("Failed to save kline", e);
                    }

                    // 调用外部回调
                    Consumer<Kline> externalCallback = externalCallbacks.get(key);
                    if (externalCallback != null) {
                        try {
                            externalCallback.accept(kline);
                        } catch (Exception e) {
                            log.error("External callback error", e);
                        }
                    }
                });
            } else {
                // 其他交易所暂时使用普通订阅（未来可扩展）
                adapter.subscribeKline(symbol, interval, kline -> {
                    log.debug("Received kline: {} {} close={}", kline.getSymbol(), kline.getInterval(), kline.getClose());

                    // 保存到数据库
                    try {
                        klineRepository.save(kline);
                    } catch (Exception e) {
                        log.error("Failed to save kline", e);
                    }

                    // 调用外部回调
                    Consumer<Kline> externalCallback = externalCallbacks.get(key);
                    if (externalCallback != null) {
                        try {
                            externalCallback.accept(kline);
                        } catch (Exception e) {
                            log.error("External callback error", e);
                        }
                    }
                });
            }
        } else {
            // 不保存到数据库，直接订阅
            adapter.subscribeKline(symbol, interval, kline -> {
                log.debug("Received kline: {} {} close={}", kline.getSymbol(), kline.getInterval(), kline.getClose());

                // 调用外部回调
                Consumer<Kline> externalCallback = externalCallbacks.get(key);
                if (externalCallback != null) {
                    try {
                        externalCallback.accept(kline);
                    } catch (Exception e) {
                        log.error("External callback error", e);
                    }
                }
            });
        }

        activeSubscriptions.add(key);
        log.info("Subscribed to: {} via {} (saveToDb={})", key, exchange, saveToDb);
    }

    /**
     * 简化的订阅方法（使用默认交易所，保存到数据库）
     */
    public void subscribe(String symbol, String interval) {
        subscribe(exchangeRouterService.getDefaultExchangeName(), symbol, interval, true, null);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String exchange, String symbol, String interval) {
        String key = exchange + "_" + symbol + "_" + interval;

        if (!activeSubscriptions.contains(key)) {
            log.warn("Not subscribed to: {}", key);
            return;
        }

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        adapter.unsubscribeKline(symbol, interval);

        activeSubscriptions.remove(key);
        externalCallbacks.remove(key);
        log.info("Unsubscribed from: {}", key);
    }

    /**
     * 获取活动订阅列表
     */
    public Set<String> getActiveSubscriptions() {
        return Set.copyOf(activeSubscriptions);
    }

    /**
     * 获取可用交易所
     */
    public Set<String> getAvailableExchanges() {
        return exchangeRouterService.getAvailableExchanges();
    }
}
