package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
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
 * 笔构建器单元测试
 */
@DisplayName("BiBuilder 单元测试")
class BiBuilderTest {

    private BiBuilder biBuilder;

    @BeforeEach
    void setUp() {
        biBuilder = new BiBuilder();
    }

    @Test
    @DisplayName("少于2个分型无法构成笔")
    void testLessThan2Fenxings() {
        List<Fenxing> fenxings = List.of(
                createFenxing(0, Fenxing.Type.BOTTOM, 95));
        List<MergedKline> klines = createKlineSequence(5);

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("底顶分型应构成上升笔")
    void testUpwardBi() {
        List<MergedKline> klines = createKlineSequence(10);
        List<Fenxing> fenxings = List.of(
                createFenxing(1, Fenxing.Type.BOTTOM, 90),
                createFenxing(6, Fenxing.Type.TOP, 120));

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertEquals(1, result.size());
        assertEquals(Bi.Direction.UP, result.get(0).getDirection());
        assertEquals(new BigDecimal("90"), result.get(0).getStartPrice());
        assertEquals(new BigDecimal("120"), result.get(0).getEndPrice());
    }

    @Test
    @DisplayName("顶底分型应构成下降笔")
    void testDownwardBi() {
        List<MergedKline> klines = createKlineSequence(10);
        List<Fenxing> fenxings = List.of(
                createFenxing(1, Fenxing.Type.TOP, 120),
                createFenxing(6, Fenxing.Type.BOTTOM, 90));

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertEquals(1, result.size());
        assertEquals(Bi.Direction.DOWN, result.get(0).getDirection());
    }

    @Test
    @DisplayName("多个分型应构成多笔")
    void testMultipleBis() {
        List<MergedKline> klines = createKlineSequence(20);
        List<Fenxing> fenxings = List.of(
                createFenxing(1, Fenxing.Type.BOTTOM, 90),
                createFenxing(6, Fenxing.Type.TOP, 120),
                createFenxing(11, Fenxing.Type.BOTTOM, 95),
                createFenxing(16, Fenxing.Type.TOP, 125));

        List<Bi> result = biBuilder.build(fenxings, klines);

        // 应有3笔: 上-下-上
        assertEquals(3, result.size());
        assertEquals(Bi.Direction.UP, result.get(0).getDirection());
        assertEquals(Bi.Direction.DOWN, result.get(1).getDirection());
        assertEquals(Bi.Direction.UP, result.get(2).getDirection());
    }

    @Test
    @DisplayName("笔之间应首尾相连")
    void testBisConnected() {
        List<MergedKline> klines = createKlineSequence(15);
        List<Fenxing> fenxings = List.of(
                createFenxing(1, Fenxing.Type.BOTTOM, 90),
                createFenxing(6, Fenxing.Type.TOP, 120),
                createFenxing(11, Fenxing.Type.BOTTOM, 95));

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertEquals(2, result.size());
        // 第一笔的终点应该是第二笔的起点
        assertEquals(result.get(0).getEndIndex(), result.get(1).getStartIndex());
    }

    @Test
    @DisplayName("笔应记录K线数量")
    void testBiKlineCount() {
        List<MergedKline> klines = createKlineSequence(10);
        List<Fenxing> fenxings = List.of(
                createFenxing(1, Fenxing.Type.BOTTOM, 90),
                createFenxing(7, Fenxing.Type.TOP, 120));

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertEquals(1, result.size());
        // 索引 1 到 7，共 7 根K线
        assertEquals(7, result.get(0).getKlineCount());
    }

    @Test
    @DisplayName("空分型列表应返回空笔列表")
    void testEmptyFenxings() {
        List<MergedKline> klines = createKlineSequence(5);
        List<Fenxing> fenxings = new ArrayList<>();

        List<Bi> result = biBuilder.build(fenxings, klines);

        assertTrue(result.isEmpty());
    }

    // 辅助方法：创建分型
    private Fenxing createFenxing(int index, Fenxing.Type type, double price) {
        MergedKline kline = MergedKline.builder()
                .index(index)
                .high(new BigDecimal(String.valueOf(price + 5)))
                .low(new BigDecimal(String.valueOf(price - 5)))
                .open(new BigDecimal(String.valueOf(price)))
                .close(new BigDecimal(String.valueOf(price)))
                .time(Instant.ofEpochSecond(1700000000L + index * 3600))
                .volume(new BigDecimal("1000"))
                .direction(MergedKline.Direction.UP)
                .elements(new ArrayList<>())
                .build();

        return Fenxing.builder()
                .index(index)
                .type(type)
                .price(new BigDecimal(String.valueOf(price)))
                .time(Instant.ofEpochSecond(1700000000L + index * 3600))
                .kline(kline)
                .build();
    }

    // 辅助方法：创建K线序列
    private List<MergedKline> createKlineSequence(int count) {
        List<MergedKline> klines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            klines.add(MergedKline.builder()
                    .index(i)
                    .high(new BigDecimal(String.valueOf(100 + i * 2)))
                    .low(new BigDecimal(String.valueOf(95 + i * 2)))
                    .open(new BigDecimal(String.valueOf(96 + i * 2)))
                    .close(new BigDecimal(String.valueOf(99 + i * 2)))
                    .time(Instant.ofEpochSecond(1700000000L + i * 3600))
                    .volume(new BigDecimal("1000"))
                    .direction(MergedKline.Direction.UP)
                    .elements(new ArrayList<>())
                    .build());
        }
        return klines;
    }
}
