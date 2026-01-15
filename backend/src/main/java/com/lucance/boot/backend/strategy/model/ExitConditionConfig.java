package com.lucance.boot.backend.strategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 出场条件配置
 * 包含止损、止盈和动态出场条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExitConditionConfig {

    /**
     * 止损设置
     */
    private StopLossConfig stopLoss;

    /**
     * 止盈设置
     */
    private TakeProfitConfig takeProfit;

    /**
     * 动态出场条件（可选，如出现反向信号时出场）
     */
    private ConditionGroup dynamicExit;

    /**
     * 最大持仓时间（毫秒），超时强制平仓。null表示不限制
     */
    private Long maxHoldingTimeMs;

    /**
     * 移动止损配置
     */
    private TrailingStopConfig trailingStop;

    /**
     * 止损配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopLossConfig {
        /**
         * 止损类型:
         * - fixed (固定价格)
         * - percent (百分比)
         * - fenxing (分型低点/高点)
         * - zhongshu (中枢边界)
         * - atr (ATR止损)
         */
        private String type;

        /**
         * 止损值（根据type解释）
         * - fixed: 具体价格
         * - percent: 百分比，如0.02表示2%
         * - atr: ATR倍数，如2.0表示2倍ATR
         */
        private BigDecimal value;

        /**
         * 偏移（用于fenxing/zhongshu类型）
         */
        private BigDecimal offset;

        public boolean isValid() {
            if (type == null)
                return false;
            if ("fixed".equals(type) || "percent".equals(type) || "atr".equals(type)) {
                return value != null;
            }
            return true;
        }
    }

    /**
     * 止盈配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TakeProfitConfig {
        /**
         * 止盈类型:
         * - fixed (固定价格)
         * - percent (百分比)
         * - ratio (盈亏比，相对于止损距离)
         * - target (目标价，如zhongshu_high)
         */
        private String type;

        /**
         * 止盈值（根据type解释）
         * - fixed: 具体价格
         * - percent: 百分比，如0.06表示6%
         * - ratio: 盈亏比，如2.0表示2:1
         */
        private BigDecimal value;

        /**
         * 分批止盈配置
         * 例如: [{ratio: 0.5, percent: 0.03}, {ratio: 0.5, percent: 0.06}]
         * 表示3%时平仓50%，6%时平仓剩余50%
         */
        private String partialExits;

        /**
         * 目标价格类型（当type=target时）
         */
        private String targetPrice;

        public boolean isValid() {
            if (type == null)
                return false;
            if ("fixed".equals(type) || "percent".equals(type) || "ratio".equals(type)) {
                return value != null;
            }
            if ("target".equals(type)) {
                return targetPrice != null;
            }
            return true;
        }
    }

    /**
     * 移动止损配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrailingStopConfig {
        /**
         * 是否启用
         */
        private Boolean enabled;

        /**
         * 激活阈值（盈利多少后激活移动止损）
         */
        private BigDecimal activationPercent;

        /**
         * 回撤幅度（从最高盈利回撤多少触发止损）
         */
        private BigDecimal trailingPercent;

        public boolean isValid() {
            if (!Boolean.TRUE.equals(enabled)) {
                return true;
            }
            return activationPercent != null && trailingPercent != null;
        }
    }

    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        if (stopLoss == null || !stopLoss.isValid()) {
            return false;
        }
        if (takeProfit != null && !takeProfit.isValid()) {
            return false;
        }
        if (trailingStop != null && !trailingStop.isValid()) {
            return false;
        }
        if (dynamicExit != null && !dynamicExit.isValid()) {
            return false;
        }
        return true;
    }

    /**
     * 获取人类可读的描述
     */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();

        if (stopLoss != null) {
            sb.append("止损: ").append(stopLoss.getType());
            if (stopLoss.getValue() != null) {
                sb.append(" ").append(stopLoss.getValue());
            }
        }

        if (takeProfit != null) {
            sb.append(", 止盈: ").append(takeProfit.getType());
            if (takeProfit.getValue() != null) {
                sb.append(" ").append(takeProfit.getValue());
            }
        }

        if (trailingStop != null && Boolean.TRUE.equals(trailingStop.getEnabled())) {
            sb.append(", 移动止损: ").append(trailingStop.getTrailingPercent());
        }

        if (maxHoldingTimeMs != null) {
            sb.append(", 最大持仓: ").append(maxHoldingTimeMs / 3600000).append("小时");
        }

        return sb.toString();
    }
}
