package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.*;
import com.lucance.boot.backend.entity.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 买卖点识别器
 * 参考: 02-phase2-chan-calculation.md
 * 
 * 买卖点分类：
 * - 1类买卖点: 趋势转折点，需要背驰判断，最强信号
 * - 2类买卖点: 回抽确认点，突破后的回测，次强信号
 * - 3类买卖点: 中枢震荡点，中枢边缘机会，风险较高
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingPointIdentifier {

    private final MACDCalculator macdCalculator;
    private final DivergenceDetector divergenceDetector;

    /**
     * 识别所有买卖点
     */
    public List<TradingPoint> identify(
            List<Bi> bis,
            List<Xianduan> xianduans,
            List<Zhongshu> zhongshus,
            List<Kline> klines) {

        List<TradingPoint> points = new ArrayList<>();

        if (bis.isEmpty() || klines.isEmpty()) {
            return points;
        }

        // 计算MACD
        List<MACDResult> macdData = macdCalculator.calculate(klines);

        // 1. 识别一类买卖点（基于背驰）
        List<TradingPoint> firstClassPoints = identifyFirstClass(xianduans, macdData, klines);
        points.addAll(firstClassPoints);

        // 2. 识别二类买卖点（回抽确认）
        List<TradingPoint> secondClassPoints = identifySecondClass(bis, xianduans, firstClassPoints);
        points.addAll(secondClassPoints);

        // 3. 识别三类买卖点（中枢震荡）
        List<TradingPoint> thirdClassPoints = identifyThirdClass(bis, zhongshus);
        points.addAll(thirdClassPoints);

        log.info("共识别 {} 个买卖点: 1类={}, 2类={}, 3类={}",
                points.size(), firstClassPoints.size(),
                secondClassPoints.size(), thirdClassPoints.size());

        return points;
    }

    /**
     * 识别一类买卖点
     * 一类买点: 下跌趋势底背驰后的低点
     * 一类卖点: 上涨趋势顶背驰后的高点
     */
    private List<TradingPoint> identifyFirstClass(
            List<Xianduan> xianduans,
            List<MACDResult> macdData,
            List<Kline> klines) {

        List<TradingPoint> points = new ArrayList<>();

        if (xianduans.size() < 2) {
            return points;
        }

        // 检测所有背驰
        List<DivergenceResult> divergences = divergenceDetector.detectAllDivergences(
                xianduans, macdData, klines);

        for (DivergenceResult divergence : divergences) {
            Xianduan xd = divergence.getXianduan2();

            TradingPoint.PointType type;
            BigDecimal price;
            String reason;

            if (divergence.getType() == DivergenceResult.DivergenceType.BEARISH) {
                // 顶背驰 -> 一类卖点
                type = TradingPoint.PointType.SELL;
                price = xd.getEndPrice();
                reason = "顶背驰形成，MACD面积递减";
            } else {
                // 底背驰 -> 一类买点
                type = TradingPoint.PointType.BUY;
                price = xd.getEndPrice();
                reason = "底背驰形成，MACD面积递减";
            }

            // 根据背驰强度确定置信度
            TradingPoint.Confidence confidence = switch (divergence.getStrength()) {
                case STRONG -> TradingPoint.Confidence.HIGH;
                case MEDIUM -> TradingPoint.Confidence.MEDIUM;
                case WEAK -> TradingPoint.Confidence.LOW;
            };

            TradingPoint point = TradingPoint.builder()
                    .id(UUID.randomUUID().toString())
                    .type(type)
                    .level(1)
                    .price(price)
                    .timestamp(xd.getEndTime())
                    .confidence(confidence)
                    .reason(reason)
                    .divergence(divergence)
                    .relatedXianduan(xd)
                    .build();

            points.add(point);
            log.debug("识别到1类{}点: price={}, confidence={}",
                    type == TradingPoint.PointType.BUY ? "买" : "卖",
                    price, confidence);
        }

        return points;
    }

    /**
     * 识别二类买卖点
     * 二类买点: 一类买点后的回抽不创新低
     * 二类卖点: 一类卖点后的反弹不创新高
     */
    private List<TradingPoint> identifySecondClass(
            List<Bi> bis,
            List<Xianduan> xianduans,
            List<TradingPoint> firstClassPoints) {

        List<TradingPoint> points = new ArrayList<>();

        if (bis.size() < 3 || firstClassPoints.isEmpty()) {
            return points;
        }

        for (TradingPoint firstClass : firstClassPoints) {
            // 查找一类买卖点之后的笔
            List<Bi> followingBis = bis.stream()
                    .filter(bi -> bi.getStartTime() >= firstClass.getTimestamp())
                    .limit(3)
                    .toList();

            if (followingBis.size() < 2) {
                continue;
            }

            // 第二根笔是回抽
            Bi secondBi = followingBis.size() > 1 ? followingBis.get(1) : null;

            if (secondBi == null) {
                continue;
            }

            if (firstClass.getType() == TradingPoint.PointType.BUY) {
                // 一类买点后，检查回抽是否不创新低
                if (secondBi.getDirection() == MergedKline.Direction.DOWN) {
                    BigDecimal pullbackLow = secondBi.getEndPrice();
                    if (pullbackLow.compareTo(firstClass.getPrice()) > 0) {
                        // 回抽不创新低，形成二类买点
                        TradingPoint point = TradingPoint.builder()
                                .id(UUID.randomUUID().toString())
                                .type(TradingPoint.PointType.BUY)
                                .level(2)
                                .price(pullbackLow)
                                .timestamp(secondBi.getEndTime())
                                .confidence(TradingPoint.Confidence.MEDIUM)
                                .reason("一类买点后回抽不创新低")
                                .relatedBi(secondBi)
                                .build();
                        points.add(point);
                    }
                }
            } else {
                // 一类卖点后，检查反弹是否不创新高
                if (secondBi.getDirection() == MergedKline.Direction.UP) {
                    BigDecimal bounceHigh = secondBi.getEndPrice();
                    if (bounceHigh.compareTo(firstClass.getPrice()) < 0) {
                        // 反弹不创新高，形成二类卖点
                        TradingPoint point = TradingPoint.builder()
                                .id(UUID.randomUUID().toString())
                                .type(TradingPoint.PointType.SELL)
                                .level(2)
                                .price(bounceHigh)
                                .timestamp(secondBi.getEndTime())
                                .confidence(TradingPoint.Confidence.MEDIUM)
                                .reason("一类卖点后反弹不创新高")
                                .relatedBi(secondBi)
                                .build();
                        points.add(point);
                    }
                }
            }
        }

        return points;
    }

    /**
     * 识别三类买卖点
     * 三类买点: 中枢下沿附近企稳
     * 三类卖点: 中枢上沿附近承压
     */
    private List<TradingPoint> identifyThirdClass(
            List<Bi> bis,
            List<Zhongshu> zhongshus) {

        List<TradingPoint> points = new ArrayList<>();

        if (bis.isEmpty() || zhongshus.isEmpty()) {
            return points;
        }

        for (Zhongshu zs : zhongshus) {
            // 查找中枢区间附近的笔
            for (Bi bi : bis) {
                // 检查笔是否在中枢时间范围附近
                if (bi.getStartTime() < zs.getStartTime() || bi.getEndTime() > zs.getEndTime() + 86400000L) {
                    continue;
                }

                BigDecimal biEndPrice = bi.getEndPrice();

                // 中枢下沿企稳 -> 三类买点
                if (bi.getDirection() == MergedKline.Direction.DOWN) {
                    // 计算到中枢下沿的距离
                    BigDecimal distanceToLow = biEndPrice.subtract(zs.getLow());
                    BigDecimal zsHeight = zs.getHeight();

                    // 如果价格在中枢下沿附近（距离 < 中枢高度的20%）
                    if (distanceToLow.abs().compareTo(zsHeight.multiply(BigDecimal.valueOf(0.2))) <= 0
                            && biEndPrice.compareTo(zs.getLow()) >= 0) {
                        TradingPoint point = TradingPoint.builder()
                                .id(UUID.randomUUID().toString())
                                .type(TradingPoint.PointType.BUY)
                                .level(3)
                                .price(biEndPrice)
                                .timestamp(bi.getEndTime())
                                .confidence(TradingPoint.Confidence.LOW)
                                .reason("中枢下沿附近企稳")
                                .relatedBi(bi)
                                .relatedZhongshu(zs)
                                .build();
                        points.add(point);
                    }
                }

                // 中枢上沿承压 -> 三类卖点
                if (bi.getDirection() == MergedKline.Direction.UP) {
                    // 计算到中枢上沿的距离
                    BigDecimal distanceToHigh = zs.getHigh().subtract(biEndPrice);
                    BigDecimal zsHeight = zs.getHeight();

                    // 如果价格在中枢上沿附近（距离 < 中枢高度的20%）
                    if (distanceToHigh.abs().compareTo(zsHeight.multiply(BigDecimal.valueOf(0.2))) <= 0
                            && biEndPrice.compareTo(zs.getHigh()) <= 0) {
                        TradingPoint point = TradingPoint.builder()
                                .id(UUID.randomUUID().toString())
                                .type(TradingPoint.PointType.SELL)
                                .level(3)
                                .price(biEndPrice)
                                .timestamp(bi.getEndTime())
                                .confidence(TradingPoint.Confidence.LOW)
                                .reason("中枢上沿附近承压")
                                .relatedBi(bi)
                                .relatedZhongshu(zs)
                                .build();
                        points.add(point);
                    }
                }
            }
        }

        return points;
    }
}
