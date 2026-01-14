package com.lucance.boot.backend.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String symbol;
    private String side; // BUY, SELL
    private String type; // MARKET, LIMIT, STOP, STOP_LIMIT
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private String timeInForce; // GTC, IOC, FOK
}
