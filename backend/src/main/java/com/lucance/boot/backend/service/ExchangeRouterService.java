package com.lucance.boot.backend.service;

import com.lucance.boot.backend.config.ExchangeProperties;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.exchange.ExchangeApiException;
import com.lucance.boot.backend.exchange.binance.BinanceAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 交易所路由服务
 * 根据交易所名称返回对应的适配器实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRouterService {

    private final ExchangeProperties exchangeProperties;
    private final LatencyStatsService latencyStatsService;

    private final Map<String, ExchangeAdapter> adapters = new HashMap<>();
    private String defaultExchange = "binance";

    @PostConstruct
    public void init() {
        // 初始化 Binance 适配器
        if (exchangeProperties.getBinance().isEnabled()) {
            BinanceAdapter binanceAdapter = new BinanceAdapter(exchangeProperties);
            binanceAdapter.setLatencyStatsService(latencyStatsService);
            adapters.put("binance", binanceAdapter);
            log.info("Binance adapter registered");
        }



        log.info("ExchangeRouterService initialized with {} adapters: {}",
                adapters.size(), adapters.keySet());
    }

    /**
     * 获取指定交易所的适配器
     * 
     * @param exchange 交易所名称 (binance, okx, bybit)
     * @return 适配器实例
     */
    public ExchangeAdapter getAdapter(String exchange) {
        String key = exchange.toLowerCase();
        ExchangeAdapter adapter = adapters.get(key);

        if (adapter == null) {
            throw new ExchangeApiException("Unsupported exchange: " + exchange +
                    ". Available: " + adapters.keySet());
        }

        return adapter;
    }

    /**
     * 获取默认交易所适配器
     */
    public ExchangeAdapter getDefaultAdapter() {
        return getAdapter(defaultExchange);
    }

    /**
     * 获取所有可用交易所名称
     */
    public Set<String> getAvailableExchanges() {
        return adapters.keySet();
    }

    /**
     * 检查交易所是否可用
     */
    public boolean isExchangeAvailable(String exchange) {
        return adapters.containsKey(exchange.toLowerCase());
    }

    /**
     * 设置默认交易所
     */
    public void setDefaultExchange(String exchange) {
        if (!isExchangeAvailable(exchange)) {
            throw new ExchangeApiException("Cannot set default to unavailable exchange: " + exchange);
        }
        this.defaultExchange = exchange.toLowerCase();
        log.info("Default exchange set to: {}", defaultExchange);
    }

    /**
     * 获取默认交易所名称
     */
    public String getDefaultExchangeName() {
        return defaultExchange;
    }

}
