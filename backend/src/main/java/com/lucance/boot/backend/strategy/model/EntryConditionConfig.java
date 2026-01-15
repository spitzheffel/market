package com.lucance.boot.backend.strategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 入场条件配置
 * 包含主要条件组和次要条件组（用于多级别共振）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryConditionConfig {

    /**
     * 主要条件组（必须满足）
     */
    private ConditionGroup primary;

    /**
     * 次要条件组（可选，用于多级别共振）
     */
    private ConditionGroup secondary;

    /**
     * 第三级条件组（可选，用于更高级别确认）
     */
    private ConditionGroup tertiary;

    /**
     * 是否要求多级别共振
     */
    @Builder.Default
    private Boolean requireResonance = false;

    /**
     * 共振时间窗口（毫秒），各级别信号需要在此时间内同时出现
     */
    @Builder.Default
    private Long resonanceWindowMs = 300000L; // 默认5分钟

    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        // 主要条件必须存在且有效
        if (primary == null || !primary.isValid()) {
            return false;
        }
        // 次要条件如果存在则必须有效
        if (secondary != null && !secondary.isValid()) {
            return false;
        }
        // 第三级条件如果存在则必须有效
        if (tertiary != null && !tertiary.isValid()) {
            return false;
        }
        return true;
    }

    /**
     * 获取人类可读的描述
     */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();

        sb.append("主条件: ").append(primary.toHumanReadable());

        if (secondary != null) {
            sb.append("\n次要条件: ").append(secondary.toHumanReadable());
        }

        if (tertiary != null) {
            sb.append("\n第三级条件: ").append(tertiary.toHumanReadable());
        }

        if (Boolean.TRUE.equals(requireResonance)) {
            sb.append("\n要求多级别共振(").append(resonanceWindowMs / 1000).append("秒内)");
        }

        return sb.toString();
    }
}
