package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 背驰检测结果
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivergenceResult {

    /**
     * 背驰类型
     */
    private DivergenceType type;

    /**
     * 价格变化率
     */
    private BigDecimal priceChange;

    /**
     * MACD变化率
     */
    private BigDecimal macdChange;

    /**
     * 成交量变化率
     */
    private BigDecimal volumeChange;

    /**
     * 背驰强度
     */
    private Strength strength;

    /**
     * 相关线段1（前一段）
     */
    private Xianduan xianduan1;

    /**
     * 相关线段2（当前段）
     */
    private Xianduan xianduan2;

    /**
     * 背驰类型枚举
     */
    public enum DivergenceType {
        BULLISH, // 底背驰（看涨）
        BEARISH // 顶背驰（看跌）
    }

    /**
     * 背驰强度枚举
     */
    public enum Strength {
        STRONG, // 强背驰
        MEDIUM, // 中等背驰
        WEAK // 弱背驰
    }

    /**
     * 判断是否为有效背驰
     */
    public boolean isValid() {
        return type != null && strength != null
                && macdChange != null
                && macdChange.abs().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否为强信号
     */
    public boolean isStrongSignal() {
        return strength == Strength.STRONG;
    }
}
