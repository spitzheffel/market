package com.lucance.boot.backend.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {
    private boolean healthy;
    private String exchange; // 交易所名称
    private String message;
    private long latency; // 延迟（毫秒）
    private long timestamp;

    public static HealthStatus healthy(String exchange, long latency) {
        return HealthStatus.builder()
                .healthy(true)
                .exchange(exchange)
                .message("OK")
                .latency(latency)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static HealthStatus unhealthy(String exchange, String message) {
        return HealthStatus.builder()
                .healthy(false)
                .exchange(exchange)
                .message(message)
                .latency(-1)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
