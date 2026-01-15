package com.lucance.boot.backend.strategy.model;

import com.lucance.boot.backend.chan.model.MACDResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * MACD条件
 * 用于检测MACD指标状态
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MACDCondition extends Condition {

    /**
     * MACD状态: golden_cross (金叉) / death_cross (死叉) /
     * positive (正值) / negative (负值) / zero_cross (穿越零轴)
     */
    private String state;

    /**
     * 是否要求柱状图放大
     */
    private Boolean histogramExpanding;

    /**
     * 是否要求柱状图缩小（背驰信号）
     */
    private Boolean histogramContracting;

    public MACDCondition(String state) {
        this.type = ConditionType.MACD;
        this.state = state;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.MACD.getCode();
    }

    @Override
    public boolean isValid() {
        if (state == null) {
            return false;
        }
        return state.equals("golden_cross") || state.equals("death_cross") ||
                state.equals("positive") || state.equals("negative") ||
                state.equals("zero_cross");
    }

    @Override
    public String toHumanReadable() {
        String stateName = switch (state) {
            case "golden_cross" -> "MACD金叉";
            case "death_cross" -> "MACD死叉";
            case "positive" -> "MACD正值";
            case "negative" -> "MACD负值";
            case "zero_cross" -> "MACD穿越零轴";
            default -> state;
        };

        StringBuilder sb = new StringBuilder(stateName);
        if (Boolean.TRUE.equals(histogramExpanding)) {
            sb.append("(柱状图放大)");
        }
        if (Boolean.TRUE.equals(histogramContracting)) {
            sb.append("(柱状图缩小)");
        }

        return sb.toString();
    }

    /**
     * 检查MACD数据是否满足条件
     */
    public boolean matches(MACDResult current, MACDResult previous) {
        if (current == null) {
            return false;
        }

        boolean stateMatch = checkState(current, previous);
        if (!stateMatch) {
            return false;
        }

        // 检查柱状图变化
        if (previous != null) {
            if (Boolean.TRUE.equals(histogramExpanding)) {
                // 柱状图绝对值应该增大
                BigDecimal currentAbs = current.getHistogram().abs();
                BigDecimal previousAbs = previous.getHistogram().abs();
                if (currentAbs.compareTo(previousAbs) <= 0) {
                    return false;
                }
            }

            if (Boolean.TRUE.equals(histogramContracting)) {
                // 柱状图绝对值应该减小
                BigDecimal currentAbs = current.getHistogram().abs();
                BigDecimal previousAbs = previous.getHistogram().abs();
                if (currentAbs.compareTo(previousAbs) >= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkState(MACDResult current, MACDResult previous) {
        return switch (state) {
            case "golden_cross" -> {
                // 金叉: DIF从下方穿越DEA
                if (previous == null)
                    yield false;
                yield previous.getDif().compareTo(previous.getDea()) < 0 &&
                        current.getDif().compareTo(current.getDea()) >= 0;
            }
            case "death_cross" -> {
                // 死叉: DIF从上方穿越DEA
                if (previous == null)
                    yield false;
                yield previous.getDif().compareTo(previous.getDea()) > 0 &&
                        current.getDif().compareTo(current.getDea()) <= 0;
            }
            case "positive" -> {
                // 正值: MACD柱状图大于0
                yield current.getHistogram().compareTo(BigDecimal.ZERO) > 0;
            }
            case "negative" -> {
                // 负值: MACD柱状图小于0
                yield current.getHistogram().compareTo(BigDecimal.ZERO) < 0;
            }
            case "zero_cross" -> {
                // 穿越零轴: DIF穿越零线
                if (previous == null)
                    yield false;
                boolean wasNegative = previous.getDif().compareTo(BigDecimal.ZERO) < 0;
                boolean isPositive = current.getDif().compareTo(BigDecimal.ZERO) >= 0;
                boolean wasPositive = previous.getDif().compareTo(BigDecimal.ZERO) > 0;
                boolean isNegative = current.getDif().compareTo(BigDecimal.ZERO) <= 0;
                yield (wasNegative && isPositive) || (wasPositive && isNegative);
            }
            default -> false;
        };
    }
}
