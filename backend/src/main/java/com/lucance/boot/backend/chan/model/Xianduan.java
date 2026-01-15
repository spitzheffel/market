package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 线段
 * 参考: 02-phase2-chan-calculation.md
 * 
 * 线段是由至少3笔构成的结构，当特征序列被破坏时线段结束。
 * 线段破坏的几种情况：
 * 1. 标准破坏：后续笔突破前面的特征区间
 * 2. 缺口破坏：出现缺口且缺口被回补
 * 3. 九笔规则：至少需要5笔才能构成线段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Xianduan {

    /**
     * 线段唯一标识
     */
    private String id;

    /**
     * 起始笔
     */
    private Bi startBi;

    /**
     * 结束笔
     */
    private Bi endBi;

    /**
     * 线段方向
     */
    private MergedKline.Direction direction;

    /**
     * 包含的所有笔
     */
    @Builder.Default
    private List<Bi> bis = new ArrayList<>();

    /**
     * 笔数量
     */
    private int biCount;

    /**
     * 起始价格
     */
    private BigDecimal startPrice;

    /**
     * 结束价格
     */
    private BigDecimal endPrice;

    /**
     * 起始时间戳
     */
    private long startTime;

    /**
     * 结束时间戳
     */
    private long endTime;

    /**
     * 是否已确认（线段结束）
     */
    private boolean confirmed;

    /**
     * 特征序列
     */
    @Builder.Default
    private List<FeatureElement> features = new ArrayList<>();

    /**
     * 线段是否被破坏
     */
    private boolean broken;

    /**
     * 获取线段高点
     */
    public BigDecimal getHigh() {
        return direction == MergedKline.Direction.UP ? endPrice : startPrice;
    }

    /**
     * 获取线段低点
     */
    public BigDecimal getLow() {
        return direction == MergedKline.Direction.UP ? startPrice : endPrice;
    }

    /**
     * 获取线段涨跌幅
     */
    public BigDecimal getChangePercent() {
        if (startPrice == null || endPrice == null
                || startPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return endPrice.subtract(startPrice)
                .divide(startPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 获取线段振幅
     */
    public BigDecimal getAmplitude() {
        if (startPrice == null || endPrice == null) {
            return BigDecimal.ZERO;
        }
        return endPrice.subtract(startPrice).abs();
    }

    /**
     * 获取线段持续时间（毫秒）
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * 判断某个价格是否在线段范围内
     */
    public boolean containsPrice(BigDecimal price) {
        if (price == null) {
            return false;
        }
        BigDecimal high = getHigh();
        BigDecimal low = getLow();
        return price.compareTo(low) >= 0 && price.compareTo(high) <= 0;
    }

    /**
     * 判断某个时间是否在线段时间范围内
     */
    public boolean containsTime(long timestamp) {
        return timestamp >= startTime && timestamp <= endTime;
    }
}
