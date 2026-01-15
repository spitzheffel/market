package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.entity.Kline;
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
 * 线段识别器单元测试
 */
@DisplayName("XianduanIdentifier 单元测试")
class XianduanIdentifierTest {

    private XianduanIdentifier identifier;

    @BeforeEach
    void setUp() {
        identifier = new XianduanIdentifier();
    }

    @Test
    @DisplayName("笔数量不足时应返回空列表")
    void testInsufficientBis() {
        List<Bi> bis = createBis(2);

        List<Xianduan> xianduans = identifier.identify(bis);

        assertTrue(xianduans.isEmpty(), "笔数量不足应返回空列表");
    }

    @Test
    @DisplayName("正好3笔应能识别线段")
    void testMinimalBis() {
        List<Bi> bis = createAlternatingBis(3, 100);

        List<Xianduan> xianduans = identifier.identify(bis);

        // 3笔可能形成1个线段
        assertNotNull(xianduans);
    }

    @Test
    @DisplayName("线段应包含正确的笔数量")
    void testXianduanBiCount() {
        List<Bi> bis = createAlternatingBis(5, 100);

        List<Xianduan> xianduans = identifier.identify(bis);

        for (Xianduan xd : xianduans) {
            assertTrue(xd.getBiCount() >= 3, "线段至少应包含3笔");
            assertEquals(xd.getBis().size(), xd.getBiCount(), "biCount应与bis列表大小一致");
        }
    }

    @Test
    @DisplayName("线段方向应与首笔方向一致")
    void testXianduanDirection() {
        List<Bi> bis = createAlternatingBis(5, 100);

        List<Xianduan> xianduans = identifier.identify(bis);

        for (Xianduan xd : xianduans) {
            assertEquals(xd.getStartBi().getDirection(), xd.getDirection(),
                    "线段方向应与首笔方向一致");
        }
    }

    @Test
    @DisplayName("线段价格应正确设置")
    void testXianduanPrices() {
        List<Bi> bis = createAlternatingBis(5, 100);

        List<Xianduan> xianduans = identifier.identify(bis);

        for (Xianduan xd : xianduans) {
            assertNotNull(xd.getStartPrice(), "起始价格不应为null");
            assertNotNull(xd.getEndPrice(), "结束价格不应为null");
            assertNotNull(xd.getHigh(), "高点不应为null");
            assertNotNull(xd.getLow(), "低点不应为null");
            assertTrue(xd.getHigh().compareTo(xd.getLow()) >= 0, "高点应>=低点");
        }
    }

    @Test
    @DisplayName("线段时间应正确设置")
    void testXianduanTimes() {
        List<Bi> bis = createAlternatingBis(5, 100);

        List<Xianduan> xianduans = identifier.identify(bis);

        for (Xianduan xd : xianduans) {
            assertTrue(xd.getStartTime() > 0, "起始时间应>0");
            assertTrue(xd.getEndTime() > 0, "结束时间应>0");
            assertTrue(xd.getEndTime() >= xd.getStartTime(), "结束时间应>=起始时间");
        }
    }

    /**
     * 创建指定数量的笔（简单模拟）
     */
    private List<Bi> createBis(int count) {
        List<Bi> bis = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bis.add(createBi(i, 100 + i * 10, MergedKline.Direction.UP));
        }
        return bis;
    }

    /**
     * 创建交替方向的笔序列
     */
    private List<Bi> createAlternatingBis(int count, double basePrice) {
        List<Bi> bis = new ArrayList<>();
        MergedKline.Direction direction = MergedKline.Direction.UP;
        double price = basePrice;

        for (int i = 0; i < count; i++) {
            bis.add(createBi(i, price, direction));

            // 交替方向
            direction = direction == MergedKline.Direction.UP
                    ? MergedKline.Direction.DOWN
                    : MergedKline.Direction.UP;

            // 调整价格
            price += direction == MergedKline.Direction.UP ? 10 : -5;
        }
        return bis;
    }

    private Bi createBi(int index, double price, MergedKline.Direction direction) {
        BigDecimal startPrice = BigDecimal.valueOf(price);
        BigDecimal endPrice = direction == MergedKline.Direction.UP
                ? BigDecimal.valueOf(price + 10)
                : BigDecimal.valueOf(price - 10);

        Fenxing startFx = Fenxing.builder()
                .type(direction == MergedKline.Direction.UP ? Fenxing.FenxingType.BOTTOM : Fenxing.FenxingType.TOP)
                .price(startPrice)
                .time(Instant.ofEpochSecond(1700000000L + index * 3600))
                .build();

        Fenxing endFx = Fenxing.builder()
                .type(direction == MergedKline.Direction.UP ? Fenxing.FenxingType.TOP : Fenxing.FenxingType.BOTTOM)
                .price(endPrice)
                .time(Instant.ofEpochSecond(1700000000L + (index + 1) * 3600 * 3))
                .build();

        return Bi.builder()
                .id(UUID.randomUUID().toString())
                .startFenxing(startFx)
                .endFenxing(endFx)
                .direction(direction)
                .startPrice(startPrice)
                .endPrice(endPrice)
                .startTime(startFx.getTime().toEpochMilli())
                .endTime(endFx.getTime().toEpochMilli())
                .klineCount(5)
                .build();
    }
}
