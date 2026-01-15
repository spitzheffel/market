package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.chan.model.Zhongshu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 中枢识别器单元测试
 */
@DisplayName("ZhongshuIdentifier 单元测试")
class ZhongshuIdentifierTest {

    private ZhongshuIdentifier identifier;

    @BeforeEach
    void setUp() {
        identifier = new ZhongshuIdentifier();
    }

    @Test
    @DisplayName("笔数量不足时应返回空列表")
    void testInsufficientBis() {
        List<Bi> bis = createBis(2, 100, 110);

        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        assertTrue(zhongshus.isEmpty(), "笔数量不足应返回空列表");
    }

    @Test
    @DisplayName("3笔有重叠时应识别出中枢")
    void testBasicZhongshu() {
        // 创建有重叠的3笔
        List<Bi> bis = createOverlappingBis(3, 100, 120);

        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        // 应该识别出1个中枢
        assertFalse(zhongshus.isEmpty(), "有重叠的3笔应形成中枢");
    }

    @Test
    @DisplayName("中枢上下轨应正确计算")
    void testZhongshuBoundaries() {
        List<Bi> bis = createOverlappingBis(3, 100, 120);

        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        for (Zhongshu zs : zhongshus) {
            assertNotNull(zs.getHigh(), "上轨不应为null");
            assertNotNull(zs.getLow(), "下轨不应为null");
            assertNotNull(zs.getCenter(), "中轨不应为null");
            assertTrue(zs.getHigh().compareTo(zs.getLow()) > 0, "上轨应>下轨");
        }
    }

    @Test
    @DisplayName("中枢级别应为BI")
    void testZhongshuLevel() {
        List<Bi> bis = createOverlappingBis(3, 100, 120);

        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        for (Zhongshu zs : zhongshus) {
            assertEquals(Zhongshu.ZhongshuLevel.BI, zs.getLevel(), "笔中枢级别应为BI");
        }
    }

    @Test
    @DisplayName("无重叠的笔不应形成中枢")
    void testNoOverlapNonZhongshu() {
        // 创建无重叠的笔
        List<Bi> bis = createNonOverlappingBis(3);

        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        assertTrue(zhongshus.isEmpty(), "无重叠的笔不应形成中枢");
    }

    @Test
    @DisplayName("价格位置判断应正确")
    void testPricePosition() {
        List<Bi> bis = createOverlappingBis(3, 100, 120);
        List<Zhongshu> zhongshus = identifier.identifyBiZhongshu(bis);

        if (!zhongshus.isEmpty()) {
            Zhongshu zs = zhongshus.get(0);

            BigDecimal above = zs.getHigh().add(BigDecimal.TEN);
            BigDecimal below = zs.getLow().subtract(BigDecimal.TEN);
            BigDecimal inside = zs.getCenter();

            assertEquals(Zhongshu.PricePosition.ABOVE, zs.getPricePosition(above));
            assertEquals(Zhongshu.PricePosition.BELOW, zs.getPricePosition(below));
            assertEquals(Zhongshu.PricePosition.INSIDE, zs.getPricePosition(inside));
        }
    }

    @Test
    @DisplayName("线段中枢识别-数量不足")
    void testXianduanZhongshuInsufficientData() {
        List<Xianduan> xianduans = createXianduans(2, 100);

        List<Zhongshu> zhongshus = identifier.identifyXianduanZhongshu(xianduans);

        assertTrue(zhongshus.isEmpty(), "线段数量不足应返回空列表");
    }

    @Test
    @DisplayName("线段中枢识别-基本功能")
    void testXianduanZhongshuBasic() {
        List<Xianduan> xianduans = createOverlappingXianduans(3, 100, 150);

        List<Zhongshu> zhongshus = identifier.identifyXianduanZhongshu(xianduans);

        for (Zhongshu zs : zhongshus) {
            assertEquals(Zhongshu.ZhongshuLevel.XIANDUAN, zs.getLevel(), "线段中枢级别应为XIANDUAN");
        }
    }

    /**
     * 创建指定数量的笔
     */
    private List<Bi> createBis(int count, double low, double high) {
        List<Bi> bis = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bis.add(createBi(i, low, high, MergedKline.Direction.UP));
        }
        return bis;
    }

    /**
     * 创建有重叠的笔序列
     */
    private List<Bi> createOverlappingBis(int count, double baseLow, double baseHigh) {
        List<Bi> bis = new ArrayList<>();
        MergedKline.Direction direction = MergedKline.Direction.UP;

        for (int i = 0; i < count; i++) {
            // 所有笔在相似的价格区间内，确保重叠
            double low = baseLow + i * 2;
            double high = baseHigh + i * 2;
            bis.add(createBi(i, low, high, direction));
            direction = direction == MergedKline.Direction.UP
                    ? MergedKline.Direction.DOWN
                    : MergedKline.Direction.UP;
        }
        return bis;
    }

    /**
     * 创建无重叠的笔序列
     */
    private List<Bi> createNonOverlappingBis(int count) {
        List<Bi> bis = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 每笔价格区间完全不重叠
            double low = 100 + i * 50;
            double high = 140 + i * 50;
            bis.add(createBi(i, low, high, MergedKline.Direction.UP));
        }
        return bis;
    }

    private Bi createBi(int index, double low, double high, MergedKline.Direction direction) {
        return Bi.builder()
                .id(UUID.randomUUID().toString())
                .direction(direction)
                .startPrice(BigDecimal.valueOf(direction == MergedKline.Direction.UP ? low : high))
                .endPrice(BigDecimal.valueOf(direction == MergedKline.Direction.UP ? high : low))
                .startTime(1700000000000L + index * 3600000)
                .endTime(1700000000000L + (index + 1) * 3600000)
                .klineCount(5)
                .build();
    }

    /**
     * 创建线段列表
     */
    private List<Xianduan> createXianduans(int count, double basePrice) {
        List<Xianduan> xianduans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            xianduans.add(createXianduan(i, basePrice + i * 10, basePrice + i * 10 + 30));
        }
        return xianduans;
    }

    /**
     * 创建有重叠的线段列表
     */
    private List<Xianduan> createOverlappingXianduans(int count, double baseLow, double baseHigh) {
        List<Xianduan> xianduans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double low = baseLow + i * 5;
            double high = baseHigh + i * 5;
            xianduans.add(createXianduan(i, low, high));
        }
        return xianduans;
    }

    private Xianduan createXianduan(int index, double low, double high) {
        return Xianduan.builder()
                .id(UUID.randomUUID().toString())
                .direction(MergedKline.Direction.UP)
                .startPrice(BigDecimal.valueOf(low))
                .endPrice(BigDecimal.valueOf(high))
                .startTime(1700000000000L + index * 7200000)
                .endTime(1700000000000L + (index + 1) * 7200000)
                .biCount(5)
                .confirmed(true)
                .build();
    }
}
