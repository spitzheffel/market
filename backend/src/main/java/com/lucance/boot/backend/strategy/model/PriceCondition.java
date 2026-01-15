package com.lucance.boot.backend.strategy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 价格条件
 * 用于检测价格水平或突破
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PriceCondition extends Condition {

    /**
     * 比较类型: above (高于) / below (低于) / between (区间内) / breakout (突破)
     */
    private String compareType;

    /**
     * 参考价格类型:
     * - fixed (固定价格)
     * - fenxing (分型价格)
     * - zhongshu_high (中枢上沿)
     * - zhongshu_low (中枢下沿)
     * - ma (均线)
     */
    private String referenceType;

    /**
     * 固定价格值（当 referenceType = fixed 时使用）
     */
    private BigDecimal value;

    /**
     * 上限值（当 compareType = between 时使用）
     */
    private BigDecimal upperValue;

    /**
     * 均线周期（当 referenceType = ma 时使用）
     */
    private Integer maPeriod;

    /**
     * 偏移百分比（例如 0.02 表示 ±2%）
     */
    private BigDecimal offsetPercent;

    public PriceCondition(String compareType, String referenceType, BigDecimal value) {
        this.type = ConditionType.PRICE;
        this.compareType = compareType;
        this.referenceType = referenceType;
        this.value = value;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.PRICE.getCode();
    }

    @Override
    public boolean isValid() {
        if (compareType == null) {
            return false;
        }
        if (!compareType.equals("above") && !compareType.equals("below") &&
                !compareType.equals("between") && !compareType.equals("breakout")) {
            return false;
        }
        if (referenceType == null) {
            return false;
        }
        if (referenceType.equals("fixed") && value == null) {
            return false;
        }
        if (compareType.equals("between") && (value == null || upperValue == null)) {
            return false;
        }
        if (referenceType.equals("ma") && (maPeriod == null || maPeriod < 1)) {
            return false;
        }
        return true;
    }

    @Override
    public String toHumanReadable() {
        String compareName = switch (compareType) {
            case "above" -> "高于";
            case "below" -> "低于";
            case "between" -> "介于";
            case "breakout" -> "突破";
            default -> compareType;
        };

        String referenceName = switch (referenceType) {
            case "fixed" -> String.valueOf(value);
            case "fenxing" -> "分型价格";
            case "zhongshu_high" -> "中枢上沿";
            case "zhongshu_low" -> "中枢下沿";
            case "ma" -> "MA" + maPeriod;
            default -> referenceType;
        };

        StringBuilder sb = new StringBuilder();
        sb.append("价格").append(compareName).append(referenceName);

        if (compareType.equals("between")) {
            sb.append("~").append(upperValue);
        }

        if (offsetPercent != null && offsetPercent.compareTo(BigDecimal.ZERO) != 0) {
            sb.append("(±").append(offsetPercent.multiply(BigDecimal.valueOf(100))).append("%)");
        }

        return sb.toString();
    }

    /**
     * 检查价格是否满足条件
     */
    public boolean matches(BigDecimal currentPrice, BigDecimal referencePrice) {
        if (currentPrice == null || referencePrice == null) {
            return false;
        }

        // 应用偏移
        BigDecimal adjustedReference = referencePrice;
        if (offsetPercent != null) {
            BigDecimal offset = referencePrice.multiply(offsetPercent);
            adjustedReference = referencePrice.add(offset);
        }

        return switch (compareType) {
            case "above" -> currentPrice.compareTo(adjustedReference) > 0;
            case "below" -> currentPrice.compareTo(adjustedReference) < 0;
            case "between" -> {
                BigDecimal upper = upperValue != null ? upperValue : adjustedReference;
                yield currentPrice.compareTo(referencePrice) >= 0 &&
                        currentPrice.compareTo(upper) <= 0;
            }
            case "breakout" -> {
                // 突破: 当前价格明显超过参考价格（超过偏移范围）
                BigDecimal breakoutThreshold = offsetPercent != null ? offsetPercent : BigDecimal.valueOf(0.005); // 默认0.5%
                BigDecimal threshold = referencePrice.multiply(breakoutThreshold);
                BigDecimal diff = currentPrice.subtract(referencePrice).abs();
                yield diff.compareTo(threshold) > 0;
            }
            default -> false;
        };
    }

    /**
     * 使用固定值检查价格
     */
    public boolean matchesWithFixedValue(BigDecimal currentPrice) {
        if ("fixed".equals(referenceType)) {
            return matches(currentPrice, value);
        }
        return false;
    }
}
