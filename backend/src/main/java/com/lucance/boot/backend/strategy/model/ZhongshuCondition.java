package com.lucance.boot.backend.strategy.model;

import com.lucance.boot.backend.chan.model.Zhongshu;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 中枢条件
 * 用于检测价格与中枢的关系
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ZhongshuCondition extends Condition {

    /**
     * 位置类型: above (中枢上方) / below (中枢下方) / inside (中枢内部) / breakout (突破)
     */
    private String position;

    /**
     * 中枢级别: bi (笔中枢) / xianduan (线段中枢)
     */
    private String zhongshuLevel;

    /**
     * 最小中枢数量
     */
    private Integer minCount;

    /**
     * 是否要求新中枢形成
     */
    private Boolean newlyFormed;

    public ZhongshuCondition(String position, String zhongshuLevel) {
        this.type = ConditionType.ZHONGSHU;
        this.position = position;
        this.zhongshuLevel = zhongshuLevel;
    }

    @Override
    public String getTypeCode() {
        return ConditionType.ZHONGSHU.getCode();
    }

    @Override
    public boolean isValid() {
        if (position == null) {
            return false;
        }
        if (!position.equals("above") && !position.equals("below") &&
                !position.equals("inside") && !position.equals("breakout")) {
            return false;
        }
        if (zhongshuLevel != null && !zhongshuLevel.equals("bi") && !zhongshuLevel.equals("xianduan")) {
            return false;
        }
        return true;
    }

    @Override
    public String toHumanReadable() {
        String positionName = switch (position) {
            case "above" -> "中枢上方";
            case "below" -> "中枢下方";
            case "inside" -> "中枢内部";
            case "breakout" -> "突破中枢";
            default -> position;
        };

        String levelName = "xianduan".equals(zhongshuLevel) ? "线段级别" : "笔级别";

        StringBuilder sb = new StringBuilder();
        sb.append("价格在").append(levelName).append(positionName);

        if (Boolean.TRUE.equals(newlyFormed)) {
            sb.append("(新形成)");
        }

        return sb.toString();
    }

    /**
     * 检查当前价格与中枢的关系
     */
    public boolean matches(BigDecimal currentPrice, List<Zhongshu> zhongshus) {
        if (currentPrice == null || zhongshus == null || zhongshus.isEmpty()) {
            return false;
        }

        // 检查中枢数量
        if (minCount != null && zhongshus.size() < minCount) {
            return false;
        }

        // 获取最近的中枢
        Zhongshu latestZs = zhongshus.get(zhongshus.size() - 1);

        return switch (position) {
            case "above" -> currentPrice.compareTo(latestZs.getHigh()) > 0;
            case "below" -> currentPrice.compareTo(latestZs.getLow()) < 0;
            case "inside" -> currentPrice.compareTo(latestZs.getLow()) >= 0 &&
                    currentPrice.compareTo(latestZs.getHigh()) <= 0;
            case "breakout" -> isBreakout(currentPrice, latestZs, zhongshus);
            default -> false;
        };
    }

    private boolean isBreakout(BigDecimal price, Zhongshu latestZs, List<Zhongshu> zhongshus) {
        // 突破判定: 价格脱离中枢区间，且是刚刚发生
        boolean isAbove = price.compareTo(latestZs.getHigh()) > 0;
        boolean isBelow = price.compareTo(latestZs.getLow()) < 0;

        // 如果有前一个中枢，检查是否是趋势性突破
        if (zhongshus.size() >= 2) {
            Zhongshu prevZs = zhongshus.get(zhongshus.size() - 2);

            // 向上突破: 当前中枢高于前一中枢
            if (isAbove && latestZs.getHigh().compareTo(prevZs.getHigh()) > 0) {
                return true;
            }

            // 向下突破: 当前中枢低于前一中枢
            if (isBelow && latestZs.getLow().compareTo(prevZs.getLow()) < 0) {
                return true;
            }
        }

        return isAbove || isBelow;
    }
}
