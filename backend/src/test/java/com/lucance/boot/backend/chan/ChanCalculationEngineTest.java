package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.TradingPoint;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.chan.model.Zhongshu;
import com.lucance.boot.backend.entity.Kline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缠论计算引擎集成测试
 */
@DisplayName("ChanCalculationEngine 集成测试")
class ChanCalculationEngineTest {

    private ChanCalculationEngine engine;

    @BeforeEach
    void setUp() {
        MACDCalculator macdCalculator = new MACDCalculator();
        DivergenceDetector divergenceDetector = new DivergenceDetector(macdCalculator);
        TradingPointIdentifier tradingPointIdentifier = new TradingPointIdentifier(macdCalculator, divergenceDetector);

        engine = new ChanCalculationEngine(
                new InclusionHandler(),
                new FenxingIdentifier(),
                new BiBuilder(),
                new XianduanIdentifier(),
                new ZhongshuIdentifier(),
                tradingPointIdentifier);
    }

    @Test
    @DisplayName("基础计算流程应返回正确结构")
    void testBasicCalculation() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        assertNotNull(result);
        assertNotNull(result.mergedKlines());
        assertNotNull(result.fenxings());
        assertNotNull(result.bis());
    }

    @Test
    @DisplayName("完整计算流程应返回正确结构（含买卖点）")
    void testFullCalculation() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        assertNotNull(result);
        assertNotNull(result.mergedKlines());
        assertNotNull(result.fenxings());
        assertNotNull(result.bis());
        assertNotNull(result.xianduans());
        assertNotNull(result.zhongshus());
        assertNotNull(result.tradingPoints()); // Phase 2: 买卖点必须非空
    }

    @Test
    @DisplayName("买卖点应包含完整的必要字段")
    void testTradingPointsCompleteness() {
        List<Kline> klines = createLargeTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        // 如果有买卖点，验证其完整性
        result.tradingPoints().forEach(tp -> {
            assertNotNull(tp.getId(), "买卖点ID不能为空");
            assertNotNull(tp.getType(), "买卖点类型不能为空");
            assertTrue(tp.getType() == TradingPoint.PointType.BUY || tp.getType() == TradingPoint.PointType.SELL,
                    "买卖点类型必须是BUY或SELL");
            assertNotNull(tp.getLevel(), "买卖点级别不能为空");
            assertTrue(tp.getLevel() >= 1 && tp.getLevel() <= 3,
                    "买卖点级别应在1-3之间");
            assertNotNull(tp.getPrice(), "买卖点价格不能为空");
            assertTrue(tp.getPrice().compareTo(BigDecimal.ZERO) > 0,
                    "买卖点价格必须大于0");
            assertTrue(tp.getTimestamp() > 0, "买卖点时间戳必须大于0");
            assertNotNull(tp.getConfidence(), "买卖点置信度不能为空");
            assertTrue(tp.getConfidence() == TradingPoint.Confidence.HIGH ||
                      tp.getConfidence() == TradingPoint.Confidence.MEDIUM ||
                      tp.getConfidence() == TradingPoint.Confidence.LOW,
                    "买卖点置信度必须是HIGH/MEDIUM/LOW之一");
            assertNotNull(tp.getReason(), "买卖点原因不能为空");
            assertFalse(tp.getReason().isEmpty(), "买卖点原因不能为空字符串");
        });
    }

    @Test
    @DisplayName("买卖点应按时间顺序排列")
    void testTradingPointsOrder() {
        List<Kline> klines = createLargeTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        var tradingPoints = result.tradingPoints();
        for (int i = 0; i < tradingPoints.size() - 1; i++) {
            assertTrue(tradingPoints.get(i).getTimestamp() <= tradingPoints.get(i + 1).getTimestamp(),
                    "买卖点应按时间顺序排列");
        }
    }

    @Test
    @DisplayName("买卖点价格应在合理范围内")
    void testTradingPointsPriceRange() {
        List<Kline> klines = createLargeTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        // 获取K线价格范围
        BigDecimal minPrice = klines.stream()
                .map(Kline::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = klines.stream()
                .map(Kline::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 验证买卖点价格在K线范围内
        result.tradingPoints().forEach(tp -> {
            assertTrue(tp.getPrice().compareTo(minPrice) >= 0,
                    "买卖点价格不应低于K线最低价");
            assertTrue(tp.getPrice().compareTo(maxPrice) <= 0,
                    "买卖点价格不应高于K线最高价");
        });
    }

    @Test
    @DisplayName("空K线列表应返回空结果")
    void testEmptyKlines() {
        List<Kline> klines = new ArrayList<>();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        assertTrue(result.mergedKlines().isEmpty());
        assertTrue(result.fenxings().isEmpty());
        assertTrue(result.bis().isEmpty());
    }

    @Test
    @DisplayName("合并后K线数量应小于等于原始数量")
    void testMergedKlinesCount() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        assertTrue(result.mergedKlines().size() <= klines.size(),
                "合并后K线数量应 <= 原始数量");
    }

    @Test
    @DisplayName("分型数量应合理")
    void testFenxingsCount() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        // 分型数量不应超过 K线数量/3
        assertTrue(result.fenxings().size() <= klines.size() / 3 + 1);
    }

    @Test
    @DisplayName("笔应由顶底分型交替构成")
    void testBisAlternation() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        List<Bi> bis = result.bis();
        for (int i = 0; i < bis.size() - 1; i++) {
            Bi current = bis.get(i);
            Bi next = bis.get(i + 1);
            // 相邻笔方向应相反
            assertNotEquals(current.getDirection(), next.getDirection(),
                    "相邻笔方向应相反");
        }
    }

    @Test
    @DisplayName("仅计算分型应正常工作")
    void testCalculateFenxingsOnly() {
        List<Kline> klines = createTestKlines();

        List<Fenxing> fenxings = engine.calculateFenxings(klines);

        assertNotNull(fenxings);
    }

    @Test
    @DisplayName("仅处理包含应正常工作")
    void testProcessMergedKlinesOnly() {
        List<Kline> klines = createTestKlines();

        List<MergedKline> merged = engine.processMergedKlines(klines);

        assertNotNull(merged);
        assertFalse(merged.isEmpty());
    }

    @Test
    @DisplayName("线段识别应正常工作")
    void testXianduanIdentification() {
        List<Kline> klines = createLargeTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        assertNotNull(result.xianduans());
        // 线段需要足够多的笔才能形成
        if (result.bis().size() >= 3) {
            // 可能有线段，也可能没有，取决于具体数据
            assertNotNull(result.xianduans());
        }
    }

    @Test
    @DisplayName("中枢识别应正常工作")
    void testZhongshuIdentification() {
        List<Kline> klines = createLargeTestKlines();

        ChanCalculationEngine.ChanResultFull result = engine.calculateFull(klines);

        assertNotNull(result.zhongshus());
        // 检查中枢的有效性
        for (Zhongshu zs : result.zhongshus()) {
            assertNotNull(zs.getHigh());
            assertNotNull(zs.getLow());
            assertTrue(zs.getHigh().compareTo(zs.getLow()) > 0,
                    "中枢上轨应大于下轨");
        }
    }

    /**
     * 创建测试用K线数据
     * 模拟一个典型的波动序列：上升 -> 下降 -> 上升
     */
    private List<Kline> createTestKlines() {
        List<Kline> klines = new ArrayList<>();

        // 上升段
        klines.add(createKline(0, 100, 105, 98, 103));
        klines.add(createKline(1, 103, 108, 101, 106));
        klines.add(createKline(2, 106, 112, 104, 110));
        klines.add(createKline(3, 110, 118, 108, 115)); // 顶部区域开始
        klines.add(createKline(4, 115, 120, 113, 118)); // 顶
        klines.add(createKline(5, 118, 119, 110, 112)); // 顶部确认

        // 下降段
        klines.add(createKline(6, 112, 114, 105, 107));
        klines.add(createKline(7, 107, 109, 100, 102));
        klines.add(createKline(8, 102, 104, 95, 97)); // 底部区域
        klines.add(createKline(9, 97, 99, 92, 94)); // 底
        klines.add(createKline(10, 94, 100, 93, 98)); // 底部确认

        // 上升段
        klines.add(createKline(11, 98, 105, 96, 103));
        klines.add(createKline(12, 103, 110, 101, 108));
        klines.add(createKline(13, 108, 115, 106, 113));
        klines.add(createKline(14, 113, 122, 111, 120));

        return klines;
    }

    /**
     * 创建更大的测试数据集，用于线段和中枢测试
     */
    private List<Kline> createLargeTestKlines() {
        List<Kline> klines = new ArrayList<>();
        int index = 0;
        double basePrice = 100;

        // 模拟多个波段
        for (int wave = 0; wave < 3; wave++) {
            // 上升波段
            for (int i = 0; i < 8; i++) {
                double open = basePrice + i * 2;
                double high = open + 3 + (i % 2);
                double low = open - 1;
                double close = open + 2;
                klines.add(createKline(index++, open, high, low, close));
            }
            basePrice += 14;

            // 下降波段
            for (int i = 0; i < 8; i++) {
                double open = basePrice - i * 2;
                double high = open + 1;
                double low = open - 3 - (i % 2);
                double close = open - 2;
                klines.add(createKline(index++, open, high, low, close));
            }
            basePrice -= 14;
        }

        return klines;
    }

    private Kline createKline(int index, double open, double high, double low, double close) {
        return Kline.builder()
                .symbol("BTCUSDT")
                .interval("1h")
                .time(Instant.ofEpochSecond(1700000000L + index * 3600))
                .open(new BigDecimal(String.valueOf(open)))
                .high(new BigDecimal(String.valueOf(high)))
                .low(new BigDecimal(String.valueOf(low)))
                .close(new BigDecimal(String.valueOf(close)))
                .volume(new BigDecimal("1000"))
                .build();
    }
}
