package com.lucance.boot.backend.backtest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单模型
 * 用于回测中模拟交易订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * 订单ID
     */
    private String id;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 订单类型: MARKET / LIMIT
     */
    private OrderType type;

    /**
     * 订单方向: BUY / SELL
     */
    private OrderSide side;

    /**
     * 订单数量
     */
    private BigDecimal quantity;

    /**
     * 订单价格（限价单使用）
     */
    private BigDecimal price;

    /**
     * 成交价格
     */
    private BigDecimal filledPrice;

    /**
     * 成交数量
     */
    private BigDecimal filledQuantity;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 成交时间
     */
    private long fillTime;

    /**
     * 手续费
     */
    private BigDecimal commission;

    /**
     * 滑点
     */
    private BigDecimal slippage;

    /**
     * 关联的策略ID
     */
    private Long strategyId;

    /**
     * 触发此订单的信号原因
     */
    private String triggerReason;

    public enum OrderType {
        MARKET,
        LIMIT
    }

    public enum OrderSide {
        BUY,
        SELL
    }

    public enum OrderStatus {
        PENDING,
        FILLED,
        PARTIALLY_FILLED,
        CANCELLED,
        REJECTED
    }

    /**
     * 计算订单总成本（包含手续费和滑点）
     */
    public BigDecimal getTotalCost() {
        if (filledPrice == null || filledQuantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseCost = filledPrice.multiply(filledQuantity);
        BigDecimal fees = commission != null ? commission : BigDecimal.ZERO;
        BigDecimal slip = slippage != null ? slippage : BigDecimal.ZERO;
        return baseCost.add(fees).add(slip);
    }

    /**
     * 检查订单是否已完成
     */
    public boolean isCompleted() {
        return status == OrderStatus.FILLED ||
                status == OrderStatus.CANCELLED ||
                status == OrderStatus.REJECTED;
    }
}
