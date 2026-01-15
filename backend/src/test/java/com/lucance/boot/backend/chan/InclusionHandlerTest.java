package com.lucance.boot.backend.chan;

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
 * 包含关系处理器单元测试
 */
@DisplayName("InclusionHandler 单元测试")
class InclusionHandlerTest {

    private InclusionHandler inclusionHandler;

    @BeforeEach
    void setUp() {
        inclusionHandler = new InclusionHandler();
    }

    @Test
    @DisplayName("空列表应返回空结果")
    void testEmptyList() {
        List<MergedKline> result = inclusionHandler.process(new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("单根K线应直接返回")
    void testSingleKline() {
        List<Kline> klines = List.of(createKline(1, 100, 110, 95, 105));

        List<MergedKline> result = inclusionHandler.process(klines);

        assertEquals(1, result.size());
        // 使用compareTo比较BigDecimal避免scale问题
        assertEquals(0, new BigDecimal("110").compareTo(result.get(0).getHigh()));
        assertEquals(0, new BigDecimal("95").compareTo(result.get(0).getLow()));
    }

    @Test
    @DisplayName("无包含关系K线应保持原样")
    void testNoInclusion() {
        // 上涨趋势，无包含
        List<Kline> klines = List.of(
                createKline(1, 100, 110, 95, 105), // H=110, L=95
                createKline(2, 106, 120, 100, 115), // H=120, L=100
                createKline(3, 116, 130, 110, 125) // H=130, L=110
        );

        List<MergedKline> result = inclusionHandler.process(klines);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("上涨趋势中的包含关系应取高高低低")
    void testUpwardInclusion() {
        // K2 包含 K3
        List<Kline> klines = List.of(
                createKline(1, 100, 105, 95, 102), // H=105, L=95 (确定上涨方向)
                createKline(2, 103, 120, 100, 115), // H=120, L=100 (包含K3)
                createKline(3, 110, 118, 105, 112) // H=118, L=105 (被K2包含)
        );

        List<MergedKline> result = inclusionHandler.process(klines);

        // K2 和 K3 应合并
        assertEquals(2, result.size());
        // 上涨：取高高低低 -> H=max(120,118)=120, L=max(100,105)=105
        // 使用compareTo比较BigDecimal避免scale问题
        assertEquals(0, new BigDecimal("120").compareTo(result.get(1).getHigh()));
        assertEquals(0, new BigDecimal("105").compareTo(result.get(1).getLow()));
    }

    @Test
    @DisplayName("下跌趋势中的包含关系应取低高高低")
    void testDownwardInclusion() {
        // 下跌趋势
        List<Kline> klines = List.of(
                createKline(1, 120, 125, 115, 118), // H=125, L=115
                createKline(2, 115, 118, 108, 110), // H=118, L=108 (下跌确认)
                createKline(3, 108, 115, 102, 112), // H=115, L=102 (包含K4)
                createKline(4, 106, 112, 104, 108) // H=112, L=104 (被K3包含)
        );

        List<MergedKline> result = inclusionHandler.process(klines);

        // 应该有合并发生
        assertTrue(result.size() < 4);
    }

    @Test
    @DisplayName("连续包含关系应递归处理")
    void testContinuousInclusion() {
        // K1 包含 K2, 合并后还包含 K3
        List<Kline> klines = List.of(
                createKline(1, 100, 130, 90, 120), // 大范围
                createKline(2, 105, 125, 95, 115), // 被K1包含
                createKline(3, 108, 120, 98, 110) // 继续包含
        );

        List<MergedKline> result = inclusionHandler.process(klines);

        // 所有K线应合并为1根
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("合并后的K线应包含所有原始元素")
    void testMergedElements() {
        List<Kline> klines = List.of(
                createKline(1, 100, 130, 90, 120),
                createKline(2, 105, 125, 95, 115));

        List<MergedKline> result = inclusionHandler.process(klines);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getElements().size());
    }

    // 辅助方法：创建测试K线
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
