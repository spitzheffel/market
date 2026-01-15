package com.lucance.boot.backend.strategy.model;

import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.Xianduan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 趋势条件
 * 用于判断当前市场趋势
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TrendCondition extends Condition {

    /**
     * 趋势方向: up / down / sideways
     */
    private String direction;

    /**
     * 最小连续线段数量（用于确认趋势强度）
     */
    private Integer minXianduanCount;

    /**
     * 是否要求趋势加速
     */
    private Boolean accelerating;

    public TrendCondition(String direction, Integer minXianduanCount) {
        this.type = ConditionType.TREND;
        this.direction = direction;
        this.minXianduanCount = minXianduanCount;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.TREND.getCode();
    }

    @Override
    public boolean isValid() {
        if (direction == null) {
            return false;
        }
        if (!direction.equals("up") && !direction.equals("down") && !direction.equals("sideways")) {
            return false;
        }
        if (minXianduanCount != null && minXianduanCount < 1) {
            return false;
        }
        return true;
    }

    @Override
    public String toHumanReadable() {
        String trendName = switch (direction) {
            case "up" -> "上涨趋势";
            case "down" -> "下跌趋势";
            case "sideways" -> "震荡行情";
            default -> direction;
        };

        StringBuilder sb = new StringBuilder(trendName);
        if (minXianduanCount != null) {
            sb.append("(至少").append(minXianduanCount).append("个线段)");
        }
        if (Boolean.TRUE.equals(accelerating)) {
            sb.append("(加速)");
        }
        return sb.toString();
    }

    /**
     * 检查线段列表是否满足趋势条件
     */
    public boolean matches(List<Xianduan> xianduans) {
        if (xianduans == null || xianduans.isEmpty()) {
            return false;
        }

        // 获取最近的线段
        int count = minXianduanCount != null ? minXianduanCount : 2;
        int startIdx = Math.max(0, xianduans.size() - count);
        List<Xianduan> recentXianduans = xianduans.subList(startIdx, xianduans.size());

        if (recentXianduans.size() < count) {
            return false;
        }

        // 判断趋势方向
        return switch (direction) {
            case "up" -> isUptrend(recentXianduans);
            case "down" -> isDowntrend(recentXianduans);
            case "sideways" -> isSideways(recentXianduans);
            default -> false;
        };
    }

    private boolean isUptrend(List<Xianduan> xianduans) {
        // 上涨趋势: 高点和低点不断抬高
        for (int i = 1; i < xianduans.size(); i++) {
            Xianduan current = xianduans.get(i);
            Xianduan previous = xianduans.get(i - 1);

            // 检查是向上线段
            if (current.getDirection() == MergedKline.Direction.UP) {
                // 新高应该高于前一个向上线段的高点
                if (current.getHigh().compareTo(previous.getHigh()) <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isDowntrend(List<Xianduan> xianduans) {
        // 下跌趋势: 高点和低点不断降低
        for (int i = 1; i < xianduans.size(); i++) {
            Xianduan current = xianduans.get(i);
            Xianduan previous = xianduans.get(i - 1);

            // 检查是向下线段
            if (current.getDirection() == MergedKline.Direction.DOWN) {
                // 新低应该低于前一个向下线段的低点
                if (current.getLow().compareTo(previous.getLow()) >= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSideways(List<Xianduan> xianduans) {
        // 震荡行情: 高点和低点没有明显趋势
        Xianduan first = xianduans.get(0);
        Xianduan last = xianduans.get(xianduans.size() - 1);

        // 价格变化幅度较小（<5%）认为是震荡
        var priceChange = last.getEndPrice().subtract(first.getStartPrice())
                .divide(first.getStartPrice(), 4, java.math.RoundingMode.HALF_UP)
                .abs();

        return priceChange.doubleValue() < 0.05;
    }
}
