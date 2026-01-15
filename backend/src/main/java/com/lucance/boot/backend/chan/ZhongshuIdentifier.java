package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Zhongshu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 中枢识别器
 * 参考: 02-phase2-chan-calculation.md
 * 
 * 核心算法：
 * 1. 笔中枢识别：检测连续3笔的价格重叠区间
 * 2. 重叠区间计算：low = max(所有低点), high = min(所有高点)
 * 3. 中枢扩展：判断新笔是否仍在中枢区间内
 */
@Slf4j
@Component
public class ZhongshuIdentifier {

    /**
     * 中枢最小构成笔数
     */
    private static final int MIN_BI_COUNT = 3;

    /**
     * 从笔序列中识别笔中枢
     */
    public List<Zhongshu> identifyBiZhongshu(List<Bi> biList) {
        List<Zhongshu> zhongshus = new ArrayList<>();

        if (biList.size() < MIN_BI_COUNT) {
            log.debug("笔数量不足 {}, 无法识别中枢", biList.size());
            return zhongshus;
        }

        int i = 0;
        while (i < biList.size() - 2) {
            // 取连续3笔检查是否有重叠
            Bi bi1 = biList.get(i);
            Bi bi2 = biList.get(i + 1);
            Bi bi3 = biList.get(i + 2);

            Overlap overlap = calculateOverlap(List.of(bi1, bi2, bi3));

            if (overlap != null) {
                // 找到中枢，继续向后扩展
                List<Bi> components = new ArrayList<>(List.of(bi1, bi2, bi3));
                int j = i + 3;
                int oscillations = 1;

                while (j < biList.size()) {
                    Bi nextBi = biList.get(j);

                    // 检查新笔是否与当前中枢有重叠
                    Overlap newOverlap = calculateOverlapWithBi(overlap, nextBi);

                    if (newOverlap != null) {
                        // 仍然有重叠，继续扩展中枢
                        components.add(nextBi);
                        overlap = newOverlap;
                        oscillations++;
                        j++;
                    } else {
                        // 没有重叠，中枢结束
                        break;
                    }
                }

                // 创建中枢
                Zhongshu zhongshu = createZhongshu(components, overlap, oscillations, true);
                zhongshus.add(zhongshu);
                log.debug("识别到中枢: {} 笔, 区间 [{}, {}]",
                        components.size(), overlap.low, overlap.high);

                // 移动到中枢后的下一个位置
                i = j;
            } else {
                i++;
            }
        }

        log.info("共识别 {} 个中枢", zhongshus.size());
        return zhongshus;
    }

    /**
     * 计算多笔的重叠区间
     * 
     * @return 重叠区间，如果无重叠则返回 null
     */
    private Overlap calculateOverlap(List<Bi> components) {
        if (components.size() < MIN_BI_COUNT) {
            return null;
        }

        // 收集所有笔的高低点
        List<BigDecimal> highs = new ArrayList<>();
        List<BigDecimal> lows = new ArrayList<>();

        for (Bi bi : components) {
            highs.add(bi.getHigh());
            lows.add(bi.getLow());
        }

        // 重叠区间 = [max(所有低点), min(所有高点)]
        BigDecimal overlapLow = lows.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal overlapHigh = highs.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 如果 low > high，说明没有重叠
        if (overlapLow.compareTo(overlapHigh) >= 0) {
            return null;
        }

        return new Overlap(overlapHigh, overlapLow);
    }

    /**
     * 检查新笔与现有重叠区间是否有重叠
     */
    private Overlap calculateOverlapWithBi(Overlap currentOverlap, Bi newBi) {
        BigDecimal biHigh = newBi.getHigh();
        BigDecimal biLow = newBi.getLow();

        // 检查新笔是否与当前中枢区间有重叠
        // 重叠条件: newBi.low <= currentOverlap.high && newBi.high >= currentOverlap.low
        if (biLow.compareTo(currentOverlap.high) > 0 || biHigh.compareTo(currentOverlap.low) < 0) {
            // 无重叠
            return null;
        }

        // 计算新的重叠区间
        BigDecimal newHigh = currentOverlap.high.min(biHigh);
        BigDecimal newLow = currentOverlap.low.max(biLow);

        if (newLow.compareTo(newHigh) >= 0) {
            return null;
        }

        return new Overlap(newHigh, newLow);
    }

    /**
     * 创建中枢对象
     */
    private Zhongshu createZhongshu(List<Bi> components, Overlap overlap,
            int oscillations, boolean confirmed) {
        if (components.isEmpty() || overlap == null) {
            return null;
        }

        Bi firstBi = components.get(0);
        Bi lastBi = components.get(components.size() - 1);

        BigDecimal center = overlap.high.add(overlap.low)
                .divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);

        return Zhongshu.builder()
                .id(UUID.randomUUID().toString())
                .level(Zhongshu.ZhongshuLevel.BI)
                .biComponents(new ArrayList<>(components))
                .high(overlap.high)
                .low(overlap.low)
                .center(center)
                .startTime(firstBi.getStartTime())
                .endTime(lastBi.getEndTime())
                .type(Zhongshu.ZhongshuType.NEW)
                .oscillations(oscillations)
                .confirmed(confirmed)
                .build();
    }

    /**
     * 判断价格相对于中枢的位置
     */
    public Zhongshu.PricePosition getPricePosition(BigDecimal price, Zhongshu zhongshu) {
        return zhongshu.getPricePosition(price);
    }

    /**
     * 判断笔是否在中枢内
     */
    public boolean isBiInsideZhongshu(Bi bi, Zhongshu zhongshu) {
        return zhongshu.hasOverlapWith(bi);
    }

    /**
     * 从线段序列中识别线段中枢
     * 线段中枢是更高级别的中枢，由至少3条线段的重叠区间构成
     */
    public List<Zhongshu> identifyXianduanZhongshu(List<com.lucance.boot.backend.chan.model.Xianduan> xianduanList) {
        List<Zhongshu> zhongshus = new ArrayList<>();

        if (xianduanList.size() < MIN_BI_COUNT) {
            log.debug("线段数量不足 {}, 无法识别线段中枢", xianduanList.size());
            return zhongshus;
        }

        int i = 0;
        while (i < xianduanList.size() - 2) {
            // 取连续3线段检查是否有重叠
            var xd1 = xianduanList.get(i);
            var xd2 = xianduanList.get(i + 1);
            var xd3 = xianduanList.get(i + 2);

            XianduanOverlap overlap = calculateXianduanOverlap(List.of(xd1, xd2, xd3));

            if (overlap != null) {
                // 找到中枢，继续向后扩展
                List<com.lucance.boot.backend.chan.model.Xianduan> components = new ArrayList<>(List.of(xd1, xd2, xd3));
                int j = i + 3;
                int oscillations = 1;

                while (j < xianduanList.size()) {
                    var nextXd = xianduanList.get(j);

                    // 检查新线段是否与当前中枢有重叠
                    XianduanOverlap newOverlap = calculateOverlapWithXianduan(overlap, nextXd);

                    if (newOverlap != null) {
                        // 仍然有重叠，继续扩展中枢
                        components.add(nextXd);
                        overlap = newOverlap;
                        oscillations++;
                        j++;
                    } else {
                        // 没有重叠，中枢结束
                        break;
                    }
                }

                // 创建线段中枢
                Zhongshu zhongshu = createXianduanZhongshu(components, overlap, oscillations, true);
                if (zhongshu != null) {
                    zhongshus.add(zhongshu);
                    log.debug("识别到线段中枢: {} 线段, 区间 [{}, {}]",
                            components.size(), overlap.low, overlap.high);
                }

                // 移动到中枢后的下一个位置
                i = j;
            } else {
                i++;
            }
        }

        log.info("共识别 {} 个线段中枢", zhongshus.size());
        return zhongshus;
    }

    /**
     * 计算多线段的重叠区间
     */
    private XianduanOverlap calculateXianduanOverlap(List<com.lucance.boot.backend.chan.model.Xianduan> components) {
        if (components.size() < MIN_BI_COUNT) {
            return null;
        }

        // 收集所有线段的高低点
        List<BigDecimal> highs = new ArrayList<>();
        List<BigDecimal> lows = new ArrayList<>();

        for (var xd : components) {
            highs.add(xd.getHigh());
            lows.add(xd.getLow());
        }

        // 重叠区间 = [max(所有低点), min(所有高点)]
        BigDecimal overlapLow = lows.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal overlapHigh = highs.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 如果 low > high，说明没有重叠
        if (overlapLow.compareTo(overlapHigh) >= 0) {
            return null;
        }

        return new XianduanOverlap(overlapHigh, overlapLow);
    }

    /**
     * 检查新线段与现有重叠区间是否有重叠
     */
    private XianduanOverlap calculateOverlapWithXianduan(XianduanOverlap currentOverlap,
            com.lucance.boot.backend.chan.model.Xianduan newXd) {
        BigDecimal xdHigh = newXd.getHigh();
        BigDecimal xdLow = newXd.getLow();

        // 检查新线段是否与当前中枢区间有重叠
        if (xdLow.compareTo(currentOverlap.high) > 0 || xdHigh.compareTo(currentOverlap.low) < 0) {
            return null;
        }

        // 计算新的重叠区间
        BigDecimal newHigh = currentOverlap.high.min(xdHigh);
        BigDecimal newLow = currentOverlap.low.max(xdLow);

        if (newLow.compareTo(newHigh) >= 0) {
            return null;
        }

        return new XianduanOverlap(newHigh, newLow);
    }

    /**
     * 创建线段中枢对象
     */
    private Zhongshu createXianduanZhongshu(List<com.lucance.boot.backend.chan.model.Xianduan> components,
            XianduanOverlap overlap, int oscillations, boolean confirmed) {
        if (components.isEmpty() || overlap == null) {
            return null;
        }

        var firstXd = components.get(0);
        var lastXd = components.get(components.size() - 1);

        BigDecimal center = overlap.high.add(overlap.low)
                .divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);

        return Zhongshu.builder()
                .id(UUID.randomUUID().toString())
                .level(Zhongshu.ZhongshuLevel.XIANDUAN)
                .xianduanComponents(new ArrayList<>(components))
                .high(overlap.high)
                .low(overlap.low)
                .center(center)
                .startTime(firstXd.getStartTime())
                .endTime(lastXd.getEndTime())
                .type(Zhongshu.ZhongshuType.NEW)
                .oscillations(oscillations)
                .confirmed(confirmed)
                .build();
    }

    /**
     * 重叠区间内部类（笔中枢）
     */
    private record Overlap(BigDecimal high, BigDecimal low) {
    }

    /**
     * 重叠区间内部类（线段中枢）
     */
    private record XianduanOverlap(BigDecimal high, BigDecimal low) {
    }
}
