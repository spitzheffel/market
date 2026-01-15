package com.lucance.boot.backend.backtest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 持仓模型
 * 用于回测中跟踪持仓状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * 持仓ID
     */
    private String id;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 持仓方向: LONG / SHORT
     */
    private PositionSide side;

    /**
     * 持仓数量
     */
    private BigDecimal quantity;

    /**
     * 平均入场价格
     */
    private BigDecimal entryPrice;

    /**
     * 当前市价
     */
    private BigDecimal currentPrice;

    /**
     * 止损价格
     */
    private BigDecimal stopLoss;

    /**
     * 止盈价格
     */
    private BigDecimal takeProfit;

    /**
     * 开仓时间
     */
    private long openTime;

    /**
     * 平仓时间
     */
    private long closeTime;

    /**
     * 已实现盈亏
     */
    private BigDecimal realizedPnl;

    /**
     * 手续费累计
     */
    private BigDecimal totalCommission;

    /**
     * 持仓状态
     */
    private PositionStatus status;

    /**
     * 最高盈利价格（用于移动止损）
     */
    private BigDecimal peakPrice;

    /**
     * 关联的策略ID
     */
    private Long strategyId;

    public enum PositionSide {
        LONG,
        SHORT
    }

    public enum PositionStatus {
        OPEN,
        CLOSED,
        PARTIAL
    }

    /**
     * 计算未实现盈亏
     */
    public BigDecimal getUnrealizedPnl() {
        if (entryPrice == null || currentPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal diff = currentPrice.subtract(entryPrice);
        if (side == PositionSide.SHORT) {
            diff = diff.negate();
        }
        return diff.multiply(quantity);
    }

    /**
     * 计算未实现盈亏比例
     */
    public BigDecimal getUnrealizedPnlPercent() {
        if (entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal pnl = getUnrealizedPnl();
        BigDecimal cost = entryPrice.multiply(quantity);
        return pnl.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * 检查是否应该止损
     */
    public boolean shouldStopLoss() {
        if (stopLoss == null || currentPrice == null) {
            return false;
        }

        if (side == PositionSide.LONG) {
            return currentPrice.compareTo(stopLoss) <= 0;
        } else {
            return currentPrice.compareTo(stopLoss) >= 0;
        }
    }

    /**
     * 检查是否应该止盈
     */
    public boolean shouldTakeProfit() {
        if (takeProfit == null || currentPrice == null) {
            return false;
        }

        if (side == PositionSide.LONG) {
            return currentPrice.compareTo(takeProfit) >= 0;
        } else {
            return currentPrice.compareTo(takeProfit) <= 0;
        }
    }

    /**
     * 更新峰值价格（用于移动止损）
     */
    public void updatePeakPrice() {
        if (currentPrice == null) {
            return;
        }

        if (peakPrice == null) {
            peakPrice = currentPrice;
            return;
        }

        if (side == PositionSide.LONG) {
            if (currentPrice.compareTo(peakPrice) > 0) {
                peakPrice = currentPrice;
            }
        } else {
            if (currentPrice.compareTo(peakPrice) < 0) {
                peakPrice = currentPrice;
            }
        }
    }

    /**
     * 获取持仓时长（毫秒）
     */
    public long getHoldingDuration(long currentTime) {
        if (status == PositionStatus.CLOSED && closeTime > 0) {
            return closeTime - openTime;
        }
        return currentTime - openTime;
    }

    /**
     * 计算持仓价值
     */
    public BigDecimal getPositionValue() {
        if (currentPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.multiply(quantity);
    }
}
