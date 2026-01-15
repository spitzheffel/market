package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.MACDResult;
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
 * MACD计算器单元测试
 */
@DisplayName("MACDCalculator 单元测试")
class MACDCalculatorTest {

    private MACDCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MACDCalculator();
    }

    @Test
    @DisplayName("K线数量不足时应返回空列表")
    void testInsufficientKlines() {
        List<Kline> klines = createKlines(10);

        List<MACDResult> results = calculator.calculate(klines);

        // 不足26根K线，无法计算完整MACD
        assertTrue(results.isEmpty() || results.stream().allMatch(r -> r.getDif() == null),
                "K线不足26根时应返回空或null值");
    }

    @Test
    @DisplayName("足够K线时应计算MACD")
    void testSufficientKlines() {
        List<Kline> klines = createKlines(50);

        List<MACDResult> results = calculator.calculate(klines);

        assertEquals(50, results.size(), "结果数量应与输入K线数量相同");
    }

    @Test
    @DisplayName("MACD结果应包含DIF和DEA")
    void testMACDComponents() {
        List<Kline> klines = createKlines(50);

        List<MACDResult> results = calculator.calculate(klines);

        // 检查后面的结果是否有有效值
        boolean hasDif = results.stream().anyMatch(r -> r.getDif() != null);
        boolean hasDea = results.stream().anyMatch(r -> r.getDea() != null);
        boolean hasMacd = results.stream().anyMatch(r -> r.getMacd() != null);

        assertTrue(hasDif, "应计算出DIF值");
        assertTrue(hasDea, "应计算出DEA值");
        assertTrue(hasMacd, "应计算出MACD柱值");
    }

    @Test
    @DisplayName("金叉检测应正确")
    void testGoldenCross() {
        MACDResult prev = MACDResult.builder()
                .dif(BigDecimal.valueOf(-1))
                .dea(BigDecimal.valueOf(0))
                .build();

        MACDResult curr = MACDResult.builder()
                .dif(BigDecimal.valueOf(1))
                .dea(BigDecimal.valueOf(0))
                .build();

        assertTrue(curr.isGoldenCross(prev), "DIF从下穿上穿DEA应为金叉");
    }

    @Test
    @DisplayName("死叉检测应正确")
    void testDeathCross() {
        MACDResult prev = MACDResult.builder()
                .dif(BigDecimal.valueOf(1))
                .dea(BigDecimal.valueOf(0))
                .build();

        MACDResult curr = MACDResult.builder()
                .dif(BigDecimal.valueOf(-1))
                .dea(BigDecimal.valueOf(0))
                .build();

        assertTrue(curr.isDeathCross(prev), "DIF从上穿下穿DEA应为死叉");
    }

    @Test
    @DisplayName("MACD面积计算应正确")
    void testMACDArea() {
        List<Kline> klines = createKlines(50);
        List<MACDResult> results = calculator.calculate(klines);

        if (!results.isEmpty()) {
            long startTime = klines.get(30).getTime().toEpochMilli();
            long endTime = klines.get(40).getTime().toEpochMilli();

            BigDecimal area = calculator.calculateMACDArea(results, startTime, endTime);

            assertNotNull(area, "面积不应为null");
            assertTrue(area.compareTo(BigDecimal.ZERO) >= 0, "面积应>=0");
        }
    }

    @Test
    @DisplayName("MACD正负判断应正确")
    void testMACDPositiveNegative() {
        MACDResult positive = MACDResult.builder().macd(BigDecimal.valueOf(1)).build();
        MACDResult negative = MACDResult.builder().macd(BigDecimal.valueOf(-1)).build();
        MACDResult zero = MACDResult.builder().macd(BigDecimal.ZERO).build();

        assertTrue(positive.isPositive());
        assertFalse(positive.isNegative());

        assertFalse(negative.isPositive());
        assertTrue(negative.isNegative());

        assertFalse(zero.isPositive());
        assertFalse(zero.isNegative());
    }

    /**
     * 创建测试用K线数据（模拟上涨趋势）
     */
    private List<Kline> createKlines(int count) {
        List<Kline> klines = new ArrayList<>();
        double basePrice = 100;

        for (int i = 0; i < count; i++) {
            double price = basePrice + i * 0.5 + Math.sin(i * 0.3) * 5;
            klines.add(Kline.builder()
                    .symbol("BTCUSDT")
                    .interval("1h")
                    .time(Instant.ofEpochSecond(1700000000L + i * 3600))
                    .open(BigDecimal.valueOf(price))
                    .high(BigDecimal.valueOf(price + 2))
                    .low(BigDecimal.valueOf(price - 1))
                    .close(BigDecimal.valueOf(price + 1))
                    .volume(BigDecimal.valueOf(1000))
                    .build());
        }
        return klines;
    }
}
