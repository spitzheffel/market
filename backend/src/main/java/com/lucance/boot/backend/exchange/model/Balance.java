package com.lucance.boot.backend.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 账户余额
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {
    private String asset;
    private BigDecimal free;
    private BigDecimal locked;

    public BigDecimal getTotal() {
        return free.add(locked);
    }
}
