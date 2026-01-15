package com.lucance.boot.backend.strategy.model;

import com.lucance.boot.backend.chan.model.TradingPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 买卖点条件
 * 用于匹配指定级别和方向的买卖点
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TradingPointCondition extends Condition {

    /**
     * 买卖点方向: buy / sell
     */
    private String direction;

    /**
     * 买卖点级别: 1, 2, 3 或 null 表示任意级别
     */
    private Integer level;

    /**
     * 最小置信度: high / medium / low
     */
    private String minConfidence;

    public TradingPointCondition(String direction, Integer level, String minConfidence) {
        this.type = ConditionType.TRADING_POINT;
        this.direction = direction;
        this.level = level;
        this.minConfidence = minConfidence;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.TRADING_POINT.getCode();
    }

    @Override
    public boolean isValid() {
        if (direction == null) {
            return false;
        }
        if (!direction.equals("buy") && !direction.equals("sell")) {
            return false;
        }
        if (level != null && (level < 1 || level > 3)) {
            return false;
        }
        if (minConfidence != null &&
                !minConfidence.equals("high") &&
                !minConfidence.equals("medium") &&
                !minConfidence.equals("low")) {
            return false;
        }
        return true;
    }

    @Override
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();
        if (level != null) {
            sb.append(level).append("类");
        }
        sb.append("buy".equals(direction) ? "买点" : "卖点");
        if (minConfidence != null) {
            sb.append("(").append(getConfidenceLabel()).append("以上)");
        }
        return sb.toString();
    }

    private String getConfidenceLabel() {
        return switch (minConfidence) {
            case "high" -> "高置信度";
            case "medium" -> "中等置信度";
            case "low" -> "低置信度";
            default -> minConfidence;
        };
    }

    /**
     * 检查买卖点是否满足此条件
     */
    public boolean matches(TradingPoint point) {
        if (point == null) {
            return false;
        }

        // 检查方向
        boolean directionMatch = switch (direction) {
            case "buy" -> point.getType() == TradingPoint.PointType.BUY;
            case "sell" -> point.getType() == TradingPoint.PointType.SELL;
            default -> false;
        };
        if (!directionMatch) {
            return false;
        }

        // 检查级别
        if (level != null && point.getLevel() != level) {
            return false;
        }

        // 检查置信度
        if (minConfidence != null) {
            int requiredLevel = getConfidenceLevel(minConfidence);
            int actualLevel = getConfidenceLevel(point.getConfidence().name().toLowerCase());
            if (actualLevel < requiredLevel) {
                return false;
            }
        }

        return true;
    }

    private int getConfidenceLevel(String confidence) {
        return switch (confidence) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }
}
