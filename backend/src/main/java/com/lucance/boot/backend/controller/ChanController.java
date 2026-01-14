package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.chan.ChanCalculationEngine;
import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.repository.KlineRepository;
import com.lucance.boot.backend.service.ExchangeRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 缠论计算控制器
 * 支持多交易所路由
 */
@Slf4j
@RestController
@RequestMapping("/api/chan")
@RequiredArgsConstructor
public class ChanController {

    private final ChanCalculationEngine chanEngine;
    private final KlineRepository klineRepository;
    private final ExchangeRouterService exchangeRouterService;

    /**
     * 完整缠论计算
     */
    @GetMapping("/calculate")
    public ResponseEntity<ChanCalculationEngine.ChanResult> calculate(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Chan calculation request: symbol={}, interval={}, limit={}, exchange={}",
                symbol, interval, limit, exchange);

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);

        if (klines.isEmpty()) {
            return ResponseEntity.ok(new ChanCalculationEngine.ChanResult(
                    List.of(), List.of(), List.of()));
        }

        ChanCalculationEngine.ChanResult result = chanEngine.calculate(klines);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取分型
     */
    @GetMapping("/fenxings")
    public ResponseEntity<List<Fenxing>> getFenxings(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        List<Fenxing> fenxings = chanEngine.calculateFenxings(klines);
        return ResponseEntity.ok(fenxings);
    }

    /**
     * 获取笔
     */
    @GetMapping("/bis")
    public ResponseEntity<List<Bi>> getBis(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResult result = chanEngine.calculate(klines);
        return ResponseEntity.ok(result.bis());
    }

    /**
     * 获取处理后的K线
     */
    @GetMapping("/merged-klines")
    public ResponseEntity<List<MergedKline>> getMergedKlines(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        List<MergedKline> mergedKlines = chanEngine.processMergedKlines(klines);
        return ResponseEntity.ok(mergedKlines);
    }

    /**
     * 获取计算统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResult result = chanEngine.calculate(klines);

        return ResponseEntity.ok(Map.of(
                "exchange", exchange,
                "klineCount", klines.size(),
                "mergedKlineCount", result.mergedKlines().size(),
                "fenxingCount", result.fenxings().size(),
                "biCount", result.bis().size(),
                "topFenxingCount", result.fenxings().stream()
                        .filter(f -> f.getType() == Fenxing.FenxingType.TOP).count(),
                "bottomFenxingCount", result.fenxings().stream()
                        .filter(f -> f.getType() == Fenxing.FenxingType.BOTTOM).count()));
    }

    /**
     * 获取K线数据
     */
    private List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime,
            Integer limit, String exchange) {
        // 优先从数据库查询
        if (startTime != null && endTime != null) {
            List<Kline> klines = klineRepository.findBySymbolAndIntervalAndTimeRange(
                    symbol, interval,
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(endTime));
            if (!klines.isEmpty()) {
                return klines;
            }
        }

        // 从指定交易所获取
        ExchangeAdapter adapter = exchangeRouterService.getAdapter(exchange);
        return adapter.getKlines(symbol, interval, startTime, endTime, limit);
    }
}
