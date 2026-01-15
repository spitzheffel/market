package com.lucance.boot.backend.strategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件组
 * 用于组合多个条件，支持 AND / OR 逻辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionGroup {

    /**
     * 应用级别（时间周期）: 1m, 5m, 15m, 1h, 4h, 1d 等
     */
    private String level;

    /**
     * 条件列表
     */
    @Builder.Default
    private List<Condition> conditions = new ArrayList<>();

    /**
     * 组合逻辑: AND / OR
     */
    @Builder.Default
    private Logic logic = Logic.AND;

    /**
     * 组合逻辑枚举
     */
    public enum Logic {
        AND, // 所有条件都必须满足
        OR // 任一条件满足即可
    }

    /**
     * 添加条件
     */
    public void addCondition(Condition condition) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        conditions.add(condition);
    }

    /**
     * 验证条件组是否有效
     */
    public boolean isValid() {
        if (level == null || level.isBlank()) {
            return false;
        }
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        return conditions.stream().allMatch(Condition::isValid);
    }

    /**
     * 获取人类可读的描述
     */
    public String toHumanReadable() {
        if (conditions == null || conditions.isEmpty()) {
            return "无条件";
        }

        String logicStr = logic == Logic.AND ? " 且 " : " 或 ";
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(level).append("] ");

        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                sb.append(logicStr);
            }
            sb.append(conditions.get(i).toHumanReadable());
        }

        return sb.toString();
    }
}
