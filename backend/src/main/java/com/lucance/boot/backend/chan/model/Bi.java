package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 笔
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bi {

    private String id;
    private Fenxing startFenxing;
    private Fenxing endFenxing;
    private MergedKline.Direction direction;
    private List<MergedKline> klines;
    private int klineCount;
    private BigDecimal startPrice;
    private BigDecimal endPrice;
    private long startTime;
    private long endTime;
    private boolean confirmed;

    /**
     * 获取涨跌幅
     */
    public BigDecimal getChangePercent() {
        if (startPrice == null || endPrice == null || startPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return endPrice.subtract(startPrice)
                .divide(startPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 获取笔的高点
     */
    public BigDecimal getHigh() {
        return direction == MergedKline.Direction.UP ? endPrice : startPrice;
    }

    /**
     * 获取笔的低点
     */
    public BigDecimal getLow() {
        return direction == MergedKline.Direction.UP ? startPrice : endPrice;
    }
}
