package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.exchange.model.HealthStatus;
import com.lucance.boot.backend.exchange.model.Ticker;
import com.lucance.boot.backend.repository.KlineRepository;
import com.lucance.boot.backend.service.ExchangeRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * K线数据控制器
 * 支持多交易所路由
 */
@Slf4j
@RestController
@RequestMapping("/api/klines")
@RequiredArgsConstructor
public class KlineController {

    private final KlineRepository klineRepository;
    private final ExchangeRouterService exchangeRouterService;

    /**
     * 获取K线数据（优先从数据库，缺失则从交易所获取）
     */
    @GetMapping
    public ResponseEntity<List<Kline>> getKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Getting klines: symbol={}, interval={}, limit={}, exchange={}",
                symbol, interval, limit, exchange);

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);

        // 如果指定了时间范围，从数据库查询
        if (startTime != null && endTime != null) {
            List<Kline> klines = klineRepository.findBySymbolAndIntervalAndTimeRange(
                    symbol, interval,
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime));

            if (!klines.isEmpty()) {
                return ResponseEntity.ok(klines);
            }
        }

        // 从交易所获取
        List<Kline> klines = adapter.getKlines(symbol, interval, startTime, endTime, limit);
        return ResponseEntity.ok(klines);
    }

    /**
     * 获取最新K线
     */
    @GetMapping("/latest")
    public ResponseEntity<List<Kline>> getLatestKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        List<Kline> klines = klineRepository.findLatestKlines(symbol, interval, limit);

        if (klines.isEmpty()) {
            // 从交易所获取
            klines = adapter.getKlines(symbol, interval, null, null, limit);
        }

        return ResponseEntity.ok(klines);
    }

    /**
     * 从交易所拉取并保存K线数据
     */
    @PostMapping("/fetch")
    public ResponseEntity<Integer> fetchAndSaveKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "1000") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Fetching klines from exchange: symbol={}, interval={}, exchange={}",
                symbol, interval, exchange);

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        List<Kline> klines = adapter.getKlines(symbol, interval, startTime, endTime, limit);
        klineRepository.saveAll(klines);

        log.info("Saved {} klines from {}", klines.size(), exchange);
        return ResponseEntity.ok(klines.size());
    }

    /**
     * 获取当前行情
     */
    @GetMapping("/ticker")
    public ResponseEntity<Ticker> getTicker(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "binance") String exchange) {

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        Ticker ticker = adapter.getTicker(symbol);
        return ResponseEntity.ok(ticker);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> healthCheck(
            @RequestParam(defaultValue = "binance") String exchange) {

        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        HealthStatus status = adapter.healthCheck();
        return ResponseEntity.ok(status);
    }

    /**
     * 获取可用交易所列表
     */
    @GetMapping("/exchanges")
    public ResponseEntity<ExchangeInfo> getExchanges() {
        return ResponseEntity.ok(new ExchangeInfo(
                exchangeRouterService.getAvailableExchanges(),
                exchangeRouterService.getDefaultExchangeName()));
    }

    /**
     * 获取K线统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<KlineStats> getStats(
            @RequestParam String symbol,
            @RequestParam String interval) {

        long count = klineRepository.countBySymbolAndInterval(symbol, interval);
        Kline latest = klineRepository.findLatestKline(symbol, interval);

        return ResponseEntity.ok(new KlineStats(
                symbol,
                interval,
                count,
                latest != null ? latest.getTimestamp() : null));
    }

    public record KlineStats(String symbol, String interval, long count, Long latestTimestamp) {
    }

    public record ExchangeInfo(Set<String> available, String defaultExchange) {
    }
}
