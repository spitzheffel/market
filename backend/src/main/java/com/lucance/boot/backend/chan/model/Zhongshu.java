package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 中枢
 * 参考: 02-phase2-chan-calculation.md
 * 
 * 中枢是由至少3笔或3线段构成的重叠区间。
 * 重叠区间计算: high = min(所有高点), low = max(所有低点)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zhongshu {

    /**
     * 中枢唯一标识
     */
    private String id;

    /**
     * 中枢级别: 笔中枢或线段中枢
     */
    private ZhongshuLevel level;

    /**
     * 构成中枢的笔列表
     */
    @Builder.Default
    private List<Bi> biComponents = new ArrayList<>();

    /**
     * 构成中枢的线段列表（线段中枢时使用）
     */
    @Builder.Default
    private List<Xianduan> xianduanComponents = new ArrayList<>();

    /**
     * 上轨（ZG）
     */
    private BigDecimal high;

    /**
     * 下轨（ZD）
     */
    private BigDecimal low;

    /**
     * 中轨（中枢中心价格）
     */
    private BigDecimal center;

    /**
     * 起始时间戳
     */
    private long startTime;

    /**
     * 结束时间戳
     */
    private long endTime;

    /**
     * 中枢类型
     */
    private ZhongshuType type;

    /**
     * 震荡次数（进入中枢的次数）
     */
    private int oscillations;

    /**
     * 是否确认（中枢结束）
     */
    private boolean confirmed;

    /**
     * 中枢级别枚举
     */
    public enum ZhongshuLevel {
        BI, // 笔中枢
        XIANDUAN // 线段中枢
    }

    /**
     * 中枢类型枚举
     */
    public enum ZhongshuType {
        EXTENDING, // 中枢延伸（震荡）
        NEW, // 新生中枢
        MOVING // 中枢移动
    }

    /**
     * 获取中枢高度
     */
    public BigDecimal getHeight() {
        if (high == null || low == null) {
            return BigDecimal.ZERO;
        }
        return high.subtract(low);
    }

    /**
     * 判断价格相对于中枢的位置
     */
    public PricePosition getPricePosition(BigDecimal price) {
        if (price == null || high == null || low == null) {
            return PricePosition.UNKNOWN;
        }

        if (price.compareTo(high) > 0) {
            return PricePosition.ABOVE;
        } else if (price.compareTo(low) < 0) {
            return PricePosition.BELOW;
        } else {
            return PricePosition.INSIDE;
        }
    }

    /**
     * 判断某个时间是否在中枢时间范围内
     */
    public boolean containsTime(long timestamp) {
        return timestamp >= startTime && timestamp <= endTime;
    }

    /**
     * 判断笔是否与中枢有重叠
     */
    public boolean hasOverlapWith(Bi bi) {
        if (bi == null || high == null || low == null) {
            return false;
        }
        BigDecimal biHigh = bi.getHigh();
        BigDecimal biLow = bi.getLow();

        // 检查是否有重叠区间
        return biLow.compareTo(high) <= 0 && biHigh.compareTo(low) >= 0;
    }

    /**
     * 获取中枢持续时间（毫秒）
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * 价格位置枚举
     */
    public enum PricePosition {
        ABOVE, // 在中枢上方
        INSIDE, // 在中枢内
        BELOW, // 在中枢下方
        UNKNOWN // 未知
    }
}
