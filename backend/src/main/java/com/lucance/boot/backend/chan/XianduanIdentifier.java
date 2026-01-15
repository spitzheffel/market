package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.FeatureElement;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.Xianduan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 线段识别器
 * 参考: 02-phase2-supplement.md
 * 
 * 核心算法：
 * 1. 构建特征序列：计算相邻笔的重叠区间
 * 2. 线段破坏检测：
 * - 标准破坏：后续笔突破前面的特征区间
 * - 缺口破坏：缺口被后续笔回补
 * - 九笔规则：至少5笔才能构成线段
 */
@Slf4j
@Component
public class XianduanIdentifier {

    /**
     * 线段最小笔数
     * 九笔规则：至少5笔才能构成线段（保证有足够的特征序列进行破坏判断）
     * 参考: 02-phase2-supplement.md (line 568)
     */
    private static final int MIN_BI_COUNT = 5;

    /**
     * 从笔序列中识别线段
     */
    public List<Xianduan> identify(List<Bi> biList) {
        List<Xianduan> xianduans = new ArrayList<>();

        if (biList.size() < MIN_BI_COUNT) {
            log.debug("笔数量不足 {}, 无法识别线段", biList.size());
            return xianduans;
        }

        // 1. 构建特征序列
        List<FeatureElement> features = buildFeatureSequence(biList);
        log.debug("构建特征序列: {} 个元素", features.size());

        // 2. 识别线段
        int segmentStartIndex = 0;

        for (int i = 2; i < biList.size(); i++) {
            // 检查线段是否被破坏
            if (isSegmentBroken(features, biList, segmentStartIndex, i)) {
                // 创建线段（不包含破坏线段的那根笔）
                Xianduan segment = createSegment(biList, features, segmentStartIndex, i - 1);
                if (segment != null && segment.getBiCount() >= MIN_BI_COUNT) {
                    segment.setConfirmed(true);
                    segment.setBroken(true);
                    xianduans.add(segment);
                    log.debug("识别到线段: {} 笔, 方向: {}", segment.getBiCount(), segment.getDirection());
                }

                // 开始新线段（从破坏笔的前一根开始）
                segmentStartIndex = i - 1;
            }
        }

        // 3. 处理最后一个未完成的线段
        if (segmentStartIndex < biList.size() - 1) {
            Xianduan lastSegment = createSegment(biList, features, segmentStartIndex, biList.size() - 1);
            if (lastSegment != null && lastSegment.getBiCount() >= MIN_BI_COUNT) {
                lastSegment.setConfirmed(false); // 未完成的线段
                lastSegment.setBroken(false);
                xianduans.add(lastSegment);
                log.debug("识别到未完成线段: {} 笔", lastSegment.getBiCount());
            }
        }

        log.info("共识别 {} 条线段", xianduans.size());
        return xianduans;
    }

    /**
     * 构建特征序列
     * 特征序列是相邻笔之间的重叠区间
     */
    private List<FeatureElement> buildFeatureSequence(List<Bi> biList) {
        List<FeatureElement> features = new ArrayList<>();

        for (int i = 0; i < biList.size() - 1; i++) {
            Bi bi1 = biList.get(i);
            Bi bi2 = biList.get(i + 1);

            // 计算两笔的高低点
            BigDecimal high1 = bi1.getHigh();
            BigDecimal low1 = bi1.getLow();
            BigDecimal high2 = bi2.getHigh();
            BigDecimal low2 = bi2.getLow();

            // 计算重叠区间: high = min(两笔高点), low = max(两笔低点)
            BigDecimal overlapHigh = high1.min(high2);
            BigDecimal overlapLow = low1.max(low2);

            // 检查是否有缺口
            boolean hasGap = overlapHigh.compareTo(overlapLow) < 0;
            BigDecimal gapSize = hasGap ? overlapLow.subtract(overlapHigh) : BigDecimal.ZERO;

            FeatureElement feature = FeatureElement.builder()
                    .index(i)
                    .bi1(bi1)
                    .bi2(bi2)
                    .high(overlapHigh)
                    .low(overlapLow)
                    .hasGap(hasGap)
                    .gapSize(gapSize)
                    .build();

            features.add(feature);
        }

        return features;
    }

    /**
     * 判断线段是否被破坏
     */
    private boolean isSegmentBroken(List<FeatureElement> features, List<Bi> biList,
            int startIndex, int currentIndex) {
        // 至少需要3笔才能判断线段破坏
        if (currentIndex - startIndex < 2) {
            return false;
        }

        // 1. 检查标准破坏
        if (checkStandardBreakdown(features, biList, startIndex, currentIndex)) {
            log.trace("检测到标准破坏: start={}, current={}", startIndex, currentIndex);
            return true;
        }

        // 2. 检查缺口破坏
        if (checkGapBreakdown(features, startIndex, currentIndex)) {
            log.trace("检测到缺口破坏: start={}, current={}", startIndex, currentIndex);
            return true;
        }

        return false;
    }

    /**
     * 检查标准破坏
     * 标准破坏：后续笔突破前面的特征区间
     */
    private boolean checkStandardBreakdown(List<FeatureElement> features, List<Bi> biList,
            int startIndex, int currentIndex) {
        if (currentIndex <= startIndex + 1 || currentIndex >= biList.size()) {
            return false;
        }

        Bi currentBi = biList.get(currentIndex);

        // 确定线段方向（基于起始笔的方向）
        Bi startBi = biList.get(startIndex);
        MergedKline.Direction segmentDirection = startBi.getDirection();

        // 获取线段内的特征区间
        // 对于向上线段，检查当前笔的低点是否低于之前的某个特征区间的低点
        // 对于向下线段，检查当前笔的高点是否高于之前的某个特征区间的高点
        for (int i = startIndex; i < currentIndex - 1 && i < features.size(); i++) {
            FeatureElement prevFeature = features.get(i);

            // 跳过有缺口的特征区间
            if (prevFeature.isHasGap()) {
                continue;
            }

            if (segmentDirection == MergedKline.Direction.UP) {
                // 向上线段：检查是否跌破前面的特征序列低点
                // 需要连续两个特征序列的低点被跌破
                if (currentBi.getLow().compareTo(prevFeature.getLow()) < 0) {
                    // 找到下一个非缺口特征，检查是否也被跌破
                    for (int j = i + 1; j < currentIndex - 1 && j < features.size(); j++) {
                        FeatureElement nextFeature = features.get(j);
                        if (!nextFeature.isHasGap() &&
                                currentBi.getLow().compareTo(nextFeature.getLow()) < 0) {
                            return true;
                        }
                    }
                }
            } else {
                // 向下线段：检查是否突破前面的特征序列高点
                if (currentBi.getHigh().compareTo(prevFeature.getHigh()) > 0) {
                    // 找到下一个非缺口特征，检查是否也被突破
                    for (int j = i + 1; j < currentIndex - 1 && j < features.size(); j++) {
                        FeatureElement nextFeature = features.get(j);
                        if (!nextFeature.isHasGap() &&
                                currentBi.getHigh().compareTo(nextFeature.getHigh()) > 0) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查缺口破坏
     * 缺口破坏：存在缺口且缺口被后续笔回补
     */
    private boolean checkGapBreakdown(List<FeatureElement> features, int startIndex, int currentIndex) {
        // 查找线段内的缺口
        for (int i = startIndex; i < currentIndex && i < features.size(); i++) {
            FeatureElement feature = features.get(i);

            if (feature.isHasGap()) {
                // 检查缺口是否被后续笔回补
                for (int j = i + 1; j < currentIndex && j < features.size(); j++) {
                    FeatureElement laterFeature = features.get(j);
                    Bi laterBi = laterFeature.getBi2();

                    // 缺口区间: [feature.high, feature.low] (注意: high < low 表示缺口)
                    // 后续笔覆盖了缺口区间，则缺口被回补
                    BigDecimal biHigh = laterBi.getHigh();
                    BigDecimal biLow = laterBi.getLow();

                    // 检查笔的区间是否覆盖了缺口
                    // 缺口: high < low, 所以缺口区间是 [high, low]
                    if (biLow.compareTo(feature.getLow()) <= 0 &&
                            biHigh.compareTo(feature.getHigh()) >= 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 创建线段对象
     */
    private Xianduan createSegment(List<Bi> biList, List<FeatureElement> allFeatures,
            int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex >= biList.size() || startIndex >= endIndex) {
            return null;
        }

        List<Bi> segmentBis = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            segmentBis.add(biList.get(i));
        }

        if (segmentBis.isEmpty()) {
            return null;
        }

        Bi firstBi = segmentBis.get(0);
        Bi lastBi = segmentBis.get(segmentBis.size() - 1);

        // 获取线段对应的特征序列
        List<FeatureElement> segmentFeatures = new ArrayList<>();
        for (int i = startIndex; i < endIndex && i < allFeatures.size(); i++) {
            segmentFeatures.add(allFeatures.get(i));
        }

        // 计算线段起止价格
        BigDecimal startPrice;
        BigDecimal endPrice;

        if (firstBi.getDirection() == MergedKline.Direction.UP) {
            // 向上线段：从低点开始，到高点结束
            startPrice = firstBi.getStartPrice();
            endPrice = lastBi.getEndPrice();
        } else {
            // 向下线段：从高点开始，到低点结束
            startPrice = firstBi.getStartPrice();
            endPrice = lastBi.getEndPrice();
        }

        return Xianduan.builder()
                .id(UUID.randomUUID().toString())
                .startBi(firstBi)
                .endBi(lastBi)
                .direction(firstBi.getDirection())
                .bis(segmentBis)
                .biCount(segmentBis.size())
                .startPrice(startPrice)
                .endPrice(endPrice)
                .startTime(firstBi.getStartTime())
                .endTime(lastBi.getEndTime())
                .features(segmentFeatures)
                .confirmed(false)
                .broken(false)
                .build();
    }
}
