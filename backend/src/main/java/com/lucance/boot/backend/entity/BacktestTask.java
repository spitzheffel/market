package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Backtest task entity for running strategy backtests.
 */
@Data
@Entity
@Table(name = "backtest_tasks")
public class BacktestTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_id", nullable = false)
    private Long strategyId;

    /**
     * Symbols to backtest (comma-separated)
     */
    @Column(length = 500)
    private String symbols;

    /**
     * Intervals to use (comma-separated)
     */
    @Column(length = 100)
    private String intervals = "1m";

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "initial_capital", precision = 20, scale = 8)
    private BigDecimal initialCapital = new BigDecimal("10000");

    /**
     * Slippage percentage (e.g., 0.001 = 0.1%)
     */
    @Column(precision = 10, scale = 6)
    private BigDecimal slippage = new BigDecimal("0.001");

    /**
     * Commission percentage (e.g., 0.001 = 0.1%)
     */
    @Column(precision = 10, scale = 6)
    private BigDecimal commission = new BigDecimal("0.001");

    /**
     * Status: pending, running, completed, failed, cancelled
     */
    @Column(length = 20)
    private String status = "pending";

    /**
     * Progress percentage (0-100)
     */
    private Integer progress = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public String[] getSymbolsArray() {
        if (symbols == null || symbols.isBlank()) {
            return new String[0];
        }
        return symbols.split(",");
    }

    public void setSymbolsArray(String[] arr) {
        this.symbols = arr == null ? "" : String.join(",", arr);
    }

    public String[] getIntervalsArray() {
        if (intervals == null || intervals.isBlank()) {
            return new String[0];
        }
        return intervals.split(",");
    }

    public void setIntervalsArray(String[] arr) {
        this.intervals = arr == null ? "1m" : String.join(",", arr);
    }
}
