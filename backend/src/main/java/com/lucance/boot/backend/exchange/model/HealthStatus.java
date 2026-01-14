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
    private String message;
    private long latency; // 延迟（毫秒）
    private long timestamp;

    public static HealthStatus healthy(long latency) {
        return HealthStatus.builder()
                .healthy(true)
                .message("OK")
                .latency(latency)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static HealthStatus unhealthy(String message) {
        return HealthStatus.builder()
                .healthy(false)
                .message(message)
                .latency(-1)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
