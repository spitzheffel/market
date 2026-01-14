package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
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
        engine = new ChanCalculationEngine(
                new InclusionHandler(),
                new FenxingIdentifier(),
                new BiBuilder());
    }

    @Test
    @DisplayName("完整计算流程应返回正确结构")
    void testFullCalculation() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        assertNotNull(result);
        assertNotNull(result.mergedKlines());
        assertNotNull(result.fenxings());
        assertNotNull(result.bis());
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
    @DisplayName("笔数量应比分型数量少1")
    void testBisCount() {
        List<Kline> klines = createTestKlines();

        ChanCalculationEngine.ChanResult result = engine.calculate(klines);

        if (result.fenxings().size() >= 2) {
            assertEquals(result.fenxings().size() - 1, result.bis().size(),
                    "笔数量应等于分型数量-1");
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
