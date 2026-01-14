package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 交易所配置实体
 * API Key 和 Secret 加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exchange_configs")
public class ExchangeConfig {

    @Id
    @Column(length = 20)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_secret")
    private String apiSecret;

    /**
     * OKX 特有的 passphrase
     */
    private String passphrase;

    /**
     * 其他配置 (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String config;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
