package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 特征序列元素
 * 参考: 02-phase2-supplement.md
 * 
 * 特征序列是相邻笔之间的重叠区间，用于判断线段是否被破坏。
 * 重叠区间计算: high = min(两笔高点), low = max(两笔低点)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureElement {

    /**
     * 在特征序列中的索引
     */
    private int index;

    /**
     * 第一根笔
     */
    private Bi bi1;

    /**
     * 第二根笔（相邻的下一根）
     */
    private Bi bi2;

    /**
     * 重叠区间高点
     * 计算方式: min(bi1.high, bi2.high)
     */
    private BigDecimal high;

    /**
     * 重叠区间低点
     * 计算方式: max(bi1.low, bi2.low)
     */
    private BigDecimal low;

    /**
     * 是否存在缺口（无重叠区间）
     * 当 high < low 时，说明两笔之间存在缺口
     */
    private boolean hasGap;

    /**
     * 缺口大小
     * 计算方式: low - high (当 hasGap = true)
     */
    private BigDecimal gapSize;

    /**
     * 判断是否有效的重叠区间
     */
    public boolean hasValidOverlap() {
        return !hasGap && high != null && low != null
                && high.compareTo(low) > 0;
    }

    /**
     * 获取重叠区间的中点
     */
    public BigDecimal getCenter() {
        if (high == null || low == null) {
            return BigDecimal.ZERO;
        }
        return high.add(low).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取重叠区间的高度
     */
    public BigDecimal getHeight() {
        if (high == null || low == null) {
            return BigDecimal.ZERO;
        }
        return high.subtract(low);
    }
}
