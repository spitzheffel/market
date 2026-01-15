package com.lucance.boot.backend.strategy.model;

import com.lucance.boot.backend.chan.model.DivergenceResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 背驰条件
 * 用于检测MACD背驰信号
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DivergenceCondition extends Condition {

    /**
     * 背驰类型: bullish (底背驰) / bearish (顶背驰)
     */
    private String divergenceType;

    /**
     * 使用的指标: macd / volume
     */
    private String indicator;

    /**
     * 最小背驰强度: strong / medium / weak
     */
    private String minStrength;

    public DivergenceCondition(String divergenceType, String indicator, String minStrength) {
        this.type = ConditionType.DIVERGENCE;
        this.divergenceType = divergenceType;
        this.indicator = indicator;
        this.minStrength = minStrength;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.DIVERGENCE.getCode();
    }

    @Override
    public boolean isValid() {
        if (divergenceType == null) {
            return false;
        }
        if (!divergenceType.equals("bullish") && !divergenceType.equals("bearish")) {
            return false;
        }
        if (indicator != null && !indicator.equals("macd") && !indicator.equals("volume")) {
            return false;
        }
        if (minStrength != null &&
                !minStrength.equals("strong") &&
                !minStrength.equals("medium") &&
                !minStrength.equals("weak")) {
            return false;
        }
        return true;
    }

    @Override
    public String toHumanReadable() {
        String typeName = "bullish".equals(divergenceType) ? "底背驰" : "顶背驰";
        String indicatorName = "volume".equals(indicator) ? "成交量" : "MACD";

        StringBuilder sb = new StringBuilder();
        sb.append(indicatorName).append(typeName);

        if (minStrength != null) {
            String strengthName = switch (minStrength) {
                case "strong" -> "强";
                case "medium" -> "中等";
                case "weak" -> "弱";
                default -> minStrength;
            };
            sb.append("(").append(strengthName).append("以上)");
        }

        return sb.toString();
    }

    /**
     * 检查背驰结果是否满足条件
     */
    public boolean matches(DivergenceResult divergence) {
        if (divergence == null) {
            return false;
        }

        // 检查背驰类型
        boolean typeMatch = switch (divergenceType) {
            case "bullish" -> divergence.getType() == DivergenceResult.DivergenceType.BULLISH;
            case "bearish" -> divergence.getType() == DivergenceResult.DivergenceType.BEARISH;
            default -> false;
        };
        if (!typeMatch) {
            return false;
        }

        // 检查背驰强度
        if (minStrength != null) {
            int requiredLevel = getStrengthLevel(minStrength);
            int actualLevel = getStrengthLevel(divergence.getStrength().name().toLowerCase());
            if (actualLevel < requiredLevel) {
                return false;
            }
        }

        return true;
    }

    private int getStrengthLevel(String strength) {
        return switch (strength) {
            case "strong" -> 3;
            case "medium" -> 2;
            case "weak" -> 1;
            default -> 0;
        };
    }
}
