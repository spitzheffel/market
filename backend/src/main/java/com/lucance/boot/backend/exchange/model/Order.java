package com.lucance.boot.backend.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private String exchangeOrderId;
    private String symbol;
    private String side;
    private String type;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal filledSize;
    private BigDecimal averagePrice;
    private OrderStatus status;
    private String error;
    private long createdAt;
    private long updatedAt;
}
