package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * MACD计算结果
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MACDResult {

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * DIF值 (快线 - 慢线)
     * EMA(12) - EMA(26)
     */
    private BigDecimal dif;

    /**
     * DEA值 (信号线)
     * DIF的9日EMA
     */
    private BigDecimal dea;

    /**
     * MACD柱状值
     * (DIF - DEA) * 2
     */
    private BigDecimal macd;

    /**
     * 判断是否金叉
     */
    public boolean isGoldenCross(MACDResult previous) {
        if (previous == null || dif == null || dea == null
                || previous.getDif() == null || previous.getDea() == null) {
            return false;
        }
        // 前一个DIF < DEA, 当前DIF > DEA
        return previous.getDif().compareTo(previous.getDea()) < 0
                && dif.compareTo(dea) > 0;
    }

    /**
     * 判断是否死叉
     */
    public boolean isDeathCross(MACDResult previous) {
        if (previous == null || dif == null || dea == null
                || previous.getDif() == null || previous.getDea() == null) {
            return false;
        }
        // 前一个DIF > DEA, 当前DIF < DEA
        return previous.getDif().compareTo(previous.getDea()) > 0
                && dif.compareTo(dea) < 0;
    }

    /**
     * 判断MACD柱是否为正
     */
    public boolean isPositive() {
        return macd != null && macd.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断MACD柱是否为负
     */
    public boolean isNegative() {
        return macd != null && macd.compareTo(BigDecimal.ZERO) < 0;
    }
}
