package com.lucance.boot.backend.backtest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 回测状态
 * 跟踪回测执行过程中的所有状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestState {

    /**
     * 回测任务ID
     */
    private Long taskId;

    /**
     * 初始资金
     */
    private BigDecimal initialCapital;

    /**
     * 当前权益
     */
    private BigDecimal equity;

    /**
     * 可用余额
     */
    private BigDecimal availableBalance;

    /**
     * 当前持仓列表
     */
    @Builder.Default
    private List<Position> openPositions = new ArrayList<>();

    /**
     * 历史已平仓持仓
     */
    @Builder.Default
    private List<Position> closedPositions = new ArrayList<>();

    /**
     * 订单历史
     */
    @Builder.Default
    private List<Order> orderHistory = new ArrayList<>();

    /**
     * 权益曲线（时间戳 -> 权益）
     */
    @Builder.Default
    private List<EquityPoint> equityCurve = new ArrayList<>();

    /**
     * 峰值权益（用于计算回撤）
     */
    private BigDecimal peakEquity;

    /**
     * 当前回撤
     */
    private BigDecimal currentDrawdown;

    /**
     * 最大回撤
     */
    private BigDecimal maxDrawdown;

    /**
     * 当前时间戳
     */
    private long currentTime;

    /**
     * 回测进度 (0-100)
     */
    private int progress;

    /**
     * 累计手续费
     */
    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;

    /**
     * 累计滑点
     */
    @Builder.Default
    private BigDecimal totalSlippage = BigDecimal.ZERO;

    /**
     * 权益点记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityPoint {
        private long timestamp;
        private BigDecimal equity;
        private BigDecimal drawdown;
    }

    /**
     * 初始化回测状态
     */
    public static BacktestState initialize(Long taskId, BigDecimal initialCapital) {
        return BacktestState.builder()
                .taskId(taskId)
                .initialCapital(initialCapital)
                .equity(initialCapital)
                .availableBalance(initialCapital)
                .peakEquity(initialCapital)
                .currentDrawdown(BigDecimal.ZERO)
                .maxDrawdown(BigDecimal.ZERO)
                .openPositions(new ArrayList<>())
                .closedPositions(new ArrayList<>())
                .orderHistory(new ArrayList<>())
                .equityCurve(new ArrayList<>())
                .totalCommission(BigDecimal.ZERO)
                .totalSlippage(BigDecimal.ZERO)
                .progress(0)
                .build();
    }

    /**
     * 更新权益和回撤
     */
    public void updateEquity(BigDecimal newEquity, long timestamp) {
        this.equity = newEquity;
        this.currentTime = timestamp;

        // 更新峰值权益
        if (newEquity.compareTo(peakEquity) > 0) {
            peakEquity = newEquity;
        }

        // 计算当前回撤
        if (peakEquity.compareTo(BigDecimal.ZERO) > 0) {
            currentDrawdown = peakEquity.subtract(newEquity)
                    .divide(peakEquity, 4, java.math.RoundingMode.HALF_UP);

            // 更新最大回撤
            if (currentDrawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = currentDrawdown;
            }
        }

        // 记录权益曲线点
        equityCurve.add(EquityPoint.builder()
                .timestamp(timestamp)
                .equity(newEquity)
                .drawdown(currentDrawdown)
                .build());
    }

    /**
     * 添加订单
     */
    public void addOrder(Order order) {
        orderHistory.add(order);
        if (order.getCommission() != null) {
            totalCommission = totalCommission.add(order.getCommission());
        }
        if (order.getSlippage() != null) {
            totalSlippage = totalSlippage.add(order.getSlippage());
        }
    }

    /**
     * 开仓
     */
    public void openPosition(Position position) {
        openPositions.add(position);
        // 扣减可用余额
        BigDecimal cost = position.getEntryPrice().multiply(position.getQuantity());
        availableBalance = availableBalance.subtract(cost);
    }

    /**
     * 平仓
     */
    public void closePosition(Position position) {
        openPositions.remove(position);
        position.setStatus(Position.PositionStatus.CLOSED);
        closedPositions.add(position);

        // 返还余额 + 盈亏
        BigDecimal returnValue = position.getPositionValue();
        if (position.getRealizedPnl() != null) {
            returnValue = position.getEntryPrice().multiply(position.getQuantity())
                    .add(position.getRealizedPnl());
        }
        availableBalance = availableBalance.add(returnValue);
    }

    /**
     * 获取总持仓价值
     */
    public BigDecimal getTotalPositionValue() {
        return openPositions.stream()
                .map(Position::getPositionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取总未实现盈亏
     */
    public BigDecimal getTotalUnrealizedPnl() {
        return openPositions.stream()
                .map(Position::getUnrealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取总已实现盈亏
     */
    public BigDecimal getTotalRealizedPnl() {
        return closedPositions.stream()
                .map(p -> p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取获胜交易数
     */
    public int getWinningTrades() {
        return (int) closedPositions.stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    /**
     * 获取亏损交易数
     */
    public int getLosingTrades() {
        return (int) closedPositions.stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0)
                .count();
    }
}
