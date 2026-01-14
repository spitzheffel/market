package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.Fenxing.FenxingType;
import com.lucance.boot.backend.chan.model.MergedKline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分型识别器单元测试
 */
@DisplayName("FenxingIdentifier 单元测试")
class FenxingIdentifierTest {

    private FenxingIdentifier fenxingIdentifier;

    @BeforeEach
    void setUp() {
        fenxingIdentifier = new FenxingIdentifier();
    }

    @Test
    @DisplayName("少于3根K线无法形成分型")
    void testLessThan3Klines() {
        List<MergedKline> klines = List.of(
                createMergedKline(0, 100, 95),
                createMergedKline(1, 105, 100));

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("识别顶分型：中间K线最高")
    void testTopFenxing() {
        // 顶分型：K2 的高点最高，低点也最高
        List<MergedKline> klines = List.of(
                createMergedKline(0, 100, 95), // 左
                createMergedKline(1, 110, 105), // 中 (最高)
                createMergedKline(2, 105, 100) // 右
        );

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        assertEquals(1, result.size());
        assertEquals(FenxingType.TOP, result.get(0).getType());
        assertEquals(1, result.get(0).getCenterIndex());
    }

    @Test
    @DisplayName("识别底分型：中间K线最低")
    void testBottomFenxing() {
        // 底分型：K2 的高点最低，低点也最低
        List<MergedKline> klines = List.of(
                createMergedKline(0, 110, 105), // 左
                createMergedKline(1, 100, 95), // 中 (最低)
                createMergedKline(2, 108, 102) // 右
        );

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        assertEquals(1, result.size());
        assertEquals(FenxingType.BOTTOM, result.get(0).getType());
    }

    @Test
    @DisplayName("连续多个分型应正确识别")
    void testMultipleFenxings() {
        // 底 -> 顶 -> 底
        List<MergedKline> klines = List.of(
                createMergedKline(0, 110, 105), //
                createMergedKline(1, 100, 95), // 底分型中点
                createMergedKline(2, 108, 102), //
                createMergedKline(3, 115, 110), //
                createMergedKline(4, 120, 115), // 顶分型中点候选
                createMergedKline(5, 112, 108), //
                createMergedKline(6, 105, 100) //
        );

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        // 至少应有分型
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("分型应交替出现（顶底顶底）")
    void testAlternatingFenxings() {
        // 构造明显的顶底交替
        List<MergedKline> klines = List.of(
                createMergedKline(0, 105, 100),
                createMergedKline(1, 95, 90), // 底
                createMergedKline(2, 100, 95),
                createMergedKline(3, 108, 103),
                createMergedKline(4, 115, 110), // 顶
                createMergedKline(5, 108, 103),
                createMergedKline(6, 100, 95),
                createMergedKline(7, 92, 87), // 底
                createMergedKline(8, 98, 93));

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        // 检查交替
        for (int i = 1; i < result.size(); i++) {
            assertNotEquals(result.get(i - 1).getType(), result.get(i).getType(),
                    "分型应交替出现");
        }
    }

    @Test
    @DisplayName("分型应记录正确的价格")
    void testFenxingPrice() {
        List<MergedKline> klines = List.of(
                createMergedKline(0, 100, 95),
                createMergedKline(1, 110, 105), // 顶分型
                createMergedKline(2, 105, 100));

        List<Fenxing> result = fenxingIdentifier.identify(klines);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("110"), result.get(0).getPrice());
    }

    // 辅助方法：创建测试用的 MergedKline
    private MergedKline createMergedKline(int index, double high, double low) {
        return MergedKline.builder()
                .index(index)
                .high(new BigDecimal(String.valueOf(high)))
                .low(new BigDecimal(String.valueOf(low)))
                .open(new BigDecimal(String.valueOf(low + 1)))
                .close(new BigDecimal(String.valueOf(high - 1)))
                .time(Instant.ofEpochSecond(1700000000L + index * 3600))
                .volume(new BigDecimal("1000"))
                .direction(MergedKline.Direction.UP)
                .elements(new ArrayList<>())
                .build();
    }
}
