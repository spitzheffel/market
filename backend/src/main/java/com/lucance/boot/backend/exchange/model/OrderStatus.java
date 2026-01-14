package com.lucance.boot.backend.exchange.model;

/**
 * 订单状态
 */
public enum OrderStatus {
    PENDING,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED,
    UNKNOWN
}
