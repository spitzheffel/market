package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.chan.ChanCalculationEngine;
import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.TradingPoint;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.chan.model.Zhongshu;
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
     * 基础缠论计算（到笔）
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
        ChanCalculationEngine.ChanResult result = chanEngine.calculate(klines);
        return ResponseEntity.ok(result);
    }

    /**
     * 完整缠论计算（包含线段和中枢）
     */
    @GetMapping("/calculate-full")
    public ResponseEntity<ChanCalculationEngine.ChanResultFull> calculateFull(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Full Chan calculation request: symbol={}, interval={}, limit={}, exchange={}",
                symbol, interval, limit, exchange);

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);
        return ResponseEntity.ok(result);
    }

    /**
     * Return same-batch klines and FULL chan result (including
     * xianduan/zhongshu/tradingPoints).
     * 
     * @param lite if true, return lightweight fields only (reduce network payload)
     */
    @GetMapping("/analysis")
    public ResponseEntity<?> getAnalysis(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange,
            @RequestParam(defaultValue = "false") boolean lite) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);

        if (lite) {
            return ResponseEntity.ok(new ChanAnalysisResponseLite(klines, result));
        }
        return ResponseEntity.ok(new ChanAnalysisResponseFull(klines, result));
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
        List<Bi> bis = chanEngine.calculateBis(klines);
        return ResponseEntity.ok(bis);
    }

    /**
     * 获取线段
     */
    @GetMapping("/xianduans")
    public ResponseEntity<List<Xianduan>> getXianduans(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);
        return ResponseEntity.ok(result.xianduans());
    }

    /**
     * 获取中枢
     */
    @GetMapping("/zhongshus")
    public ResponseEntity<List<Zhongshu>> getZhongshus(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);
        return ResponseEntity.ok(result.zhongshus());
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
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);

        return ResponseEntity.ok(Map.of(
                "exchange", exchange,
                "klineCount", klines.size(),
                "mergedKlineCount", result.mergedKlines().size(),
                "fenxingCount", result.fenxings().size(),
                "biCount", result.bis().size(),
                "xianduanCount", result.xianduans().size(),
                "zhongshuCount", result.zhongshus().size(),
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

    public record ChanAnalysisResponse(List<Kline> klines, ChanCalculationEngine.ChanResult result) {
    }

    /**
     * 完整缠论分析响应（包含线段、中枢、买卖点）
     */
    public record ChanAnalysisResponseFull(List<Kline> klines, ChanCalculationEngine.ChanResultFull result) {
    }

    /**
     * 轻量缠论分析响应（仅包含渲染必要字段，减少网络负载）
     */
    public record ChanAnalysisResponseLite(
            List<KlineLite> klines,
            ChanResultLite result) {
        public ChanAnalysisResponseLite(List<Kline> klines, ChanCalculationEngine.ChanResultFull full) {
            this(
                    klines.stream().map(k -> new KlineLite(
                            k.getTime().toEpochMilli(),
                            k.getOpen(), k.getHigh(), k.getLow(), k.getClose(), k.getVolume())).toList(),
                    new ChanResultLite(full));
        }
    }

    public record KlineLite(long time, java.math.BigDecimal open, java.math.BigDecimal high,
            java.math.BigDecimal low, java.math.BigDecimal close, java.math.BigDecimal volume) {
    }

    public record ChanResultLite(
            List<MergedKlineLite> mergedKlines,
            List<FenxingLite> fenxings,
            List<BiLite> bis,
            List<XianduanLite> xianduans,
            List<ZhongshuLite> zhongshus,
            List<TradingPointLite> tradingPoints) {
        public ChanResultLite(ChanCalculationEngine.ChanResultFull full) {
            this(
                    full.mergedKlines().stream()
                            .map(m -> new MergedKlineLite(m.getIndex(), m.getTimestamp()))
                            .toList(),
                    full.fenxings().stream()
                            .map(f -> new FenxingLite(f.getCenterIndex(), f.getType().name(), f.getPrice())).toList(),
                    full.bis().stream()
                            .map(b -> new BiLite(b.getStartFenxing().getCenterIndex(),
                                    b.getEndFenxing().getCenterIndex(), b.getDirection().name(), b.getStartPrice(),
                                    b.getEndPrice()))
                            .toList(),
                    full.xianduans().stream()
                            .map(x -> new XianduanLite(x.getStartTime(), x.getEndTime(), x.getDirection().name(),
                                    x.getStartPrice(), x.getEndPrice()))
                            .toList(),
                    full.zhongshus().stream()
                            .map(z -> new ZhongshuLite(z.getStartTime(), z.getEndTime(), z.getHigh(), z.getLow(),
                                    z.getCenter()))
                            .toList(),
                    full.tradingPoints().stream().map(
                            t -> new TradingPointLite(t.getTimestamp(), t.getType().name(), t.getLevel(), t.getPrice()))
                            .toList());
        }
    }

    public record MergedKlineLite(int index, long timestamp) {
    }

    public record FenxingLite(int index, String type, java.math.BigDecimal price) {
    }

    public record BiLite(int startIndex, int endIndex, String direction, java.math.BigDecimal startPrice,
            java.math.BigDecimal endPrice) {
    }

    public record XianduanLite(long startTime, long endTime, String direction, java.math.BigDecimal startPrice,
            java.math.BigDecimal endPrice) {
    }

    public record ZhongshuLite(long startTime, long endTime, java.math.BigDecimal high, java.math.BigDecimal low,
            java.math.BigDecimal center) {
    }

    public record TradingPointLite(long timestamp, String type, int level, java.math.BigDecimal price) {
    }

    /**
     * 获取买卖点
     */
    @GetMapping("/trading-points")
    public ResponseEntity<List<TradingPoint>> getTradingPoints(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "500") Integer limit,
            @RequestParam(defaultValue = "binance") String exchange) {

        List<Kline> klines = getKlines(symbol, interval, startTime, endTime, limit, exchange);
        ChanCalculationEngine.ChanResultFull result = chanEngine.calculateFull(klines);
        return ResponseEntity.ok(result.tradingPoints());
    }
}
