package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.DivergenceResult;
import com.lucance.boot.backend.chan.model.MACDResult;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.entity.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 背驰检测器
 * 参考: 02-phase2-chan-calculation.md
 * 
 * 背驰判定条件：
 * - 顶背驰: 价格创新高，但MACD面积缩小
 * - 底背驰: 价格创新低，但MACD面积缩小（绝对值）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DivergenceDetector {

    private final MACDCalculator macdCalculator;

    /**
     * 检测两个线段之间的背驰
     */
    public DivergenceResult detectDivergence(
            Xianduan xianduan1,
            Xianduan xianduan2,
            List<MACDResult> macdData,
            List<Kline> klines) {

        if (xianduan1 == null || xianduan2 == null || macdData.isEmpty()) {
            return null;
        }

        // 确保两个线段方向相同
        if (xianduan1.getDirection() != xianduan2.getDirection()) {
            return null;
        }

        // 计算价格变化
        BigDecimal price1 = xianduan1.getEndPrice();
        BigDecimal price2 = xianduan2.getEndPrice();

        // 计算MACD面积
        BigDecimal macd1Area = macdCalculator.calculateMACDArea(
                macdData, xianduan1.getStartTime(), xianduan1.getEndTime());
        BigDecimal macd2Area = macdCalculator.calculateMACDArea(
                macdData, xianduan2.getStartTime(), xianduan2.getEndTime());

        // 计算成交量
        BigDecimal volume1 = calculateVolume(klines, xianduan1.getStartTime(), xianduan1.getEndTime());
        BigDecimal volume2 = calculateVolume(klines, xianduan2.getStartTime(), xianduan2.getEndTime());

        // 判断背驰
        if (xianduan1.getDirection() == MergedKline.Direction.UP) {
            // 上涨线段，检查顶背驰
            // 条件: 价格新高但MACD面积减少
            if (price2.compareTo(price1) > 0 && macd2Area.compareTo(macd1Area) < 0) {
                return buildDivergenceResult(
                        DivergenceResult.DivergenceType.BEARISH,
                        price1, price2,
                        macd1Area, macd2Area,
                        volume1, volume2,
                        xianduan1, xianduan2);
            }
        } else {
            // 下跌线段，检查底背驰
            // 条件: 价格新低但MACD面积减少
            if (price2.compareTo(price1) < 0 && macd2Area.compareTo(macd1Area) < 0) {
                return buildDivergenceResult(
                        DivergenceResult.DivergenceType.BULLISH,
                        price1, price2,
                        macd1Area, macd2Area,
                        volume1, volume2,
                        xianduan1, xianduan2);
            }
        }

        return null;
    }

    /**
     * 检测线段列表中的所有背驰
     */
    public List<DivergenceResult> detectAllDivergences(
            List<Xianduan> xianduans,
            List<MACDResult> macdData,
            List<Kline> klines) {

        List<DivergenceResult> divergences = new ArrayList<>();

        if (xianduans.size() < 2) {
            return divergences;
        }

        // 检查同方向的相邻线段对
        for (int i = 0; i < xianduans.size() - 2; i++) {
            Xianduan xd1 = xianduans.get(i);
            Xianduan xd2 = xianduans.get(i + 2); // 跳过中间的反向线段

            if (xd1.getDirection() == xd2.getDirection()) {
                DivergenceResult divergence = detectDivergence(xd1, xd2, macdData, klines);
                if (divergence != null) {
                    divergences.add(divergence);
                    log.debug("检测到{}背驰: {} -> {}",
                            divergence.getType() == DivergenceResult.DivergenceType.BEARISH ? "顶" : "底",
                            xd1.getId(), xd2.getId());
                }
            }
        }

        log.info("共检测到 {} 处背驰", divergences.size());
        return divergences;
    }

    /**
     * 构建背驰结果
     */
    private DivergenceResult buildDivergenceResult(
            DivergenceResult.DivergenceType type,
            BigDecimal price1, BigDecimal price2,
            BigDecimal macd1, BigDecimal macd2,
            BigDecimal volume1, BigDecimal volume2,
            Xianduan xianduan1, Xianduan xianduan2) {

        // 计算变化率
        BigDecimal priceChange = BigDecimal.ZERO;
        if (price1.compareTo(BigDecimal.ZERO) != 0) {
            priceChange = price2.subtract(price1)
                    .divide(price1, 4, RoundingMode.HALF_UP);
        }

        BigDecimal macdChange = BigDecimal.ZERO;
        if (macd1.compareTo(BigDecimal.ZERO) != 0) {
            macdChange = macd2.subtract(macd1)
                    .divide(macd1, 4, RoundingMode.HALF_UP);
        }

        BigDecimal volumeChange = BigDecimal.ZERO;
        if (volume1.compareTo(BigDecimal.ZERO) != 0) {
            volumeChange = volume2.subtract(volume1)
                    .divide(volume1, 4, RoundingMode.HALF_UP);
        }

        // 计算背驰强度
        DivergenceResult.Strength strength = calculateStrength(priceChange, macdChange);

        return DivergenceResult.builder()
                .type(type)
                .priceChange(priceChange)
                .macdChange(macdChange)
                .volumeChange(volumeChange)
                .strength(strength)
                .xianduan1(xianduan1)
                .xianduan2(xianduan2)
                .build();
    }

    /**
     * 计算背驰强度
     */
    private DivergenceResult.Strength calculateStrength(BigDecimal priceChange, BigDecimal macdChange) {
        if (priceChange.compareTo(BigDecimal.ZERO) == 0) {
            return DivergenceResult.Strength.WEAK;
        }

        // 计算背驰比率: MACD变化 / 价格变化
        BigDecimal ratio = macdChange.abs().divide(priceChange.abs(), 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(BigDecimal.valueOf(2)) > 0) {
            return DivergenceResult.Strength.STRONG;
        } else if (ratio.compareTo(BigDecimal.ONE) > 0) {
            return DivergenceResult.Strength.MEDIUM;
        } else {
            return DivergenceResult.Strength.WEAK;
        }
    }

    /**
     * 计算指定时间范围内的成交量
     */
    private BigDecimal calculateVolume(List<Kline> klines, long startTime, long endTime) {
        return klines.stream()
                .filter(k -> k.getTime().toEpochMilli() >= startTime
                        && k.getTime().toEpochMilli() <= endTime)
                .map(Kline::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
