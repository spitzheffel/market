package com.lucance.boot.backend.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 行情数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticker {
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private long timestamp;
}
