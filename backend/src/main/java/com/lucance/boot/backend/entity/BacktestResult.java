package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Backtest result entity storing performance metrics and detailed data.
 */
@Data
@Entity
@Table(name = "backtest_results")
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    // ============ Return Metrics ============

    /**
     * Total return percentage
     */
    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    /**
     * Annualized return percentage
     */
    @Column(name = "annualized_return", precision = 10, scale = 4)
    private BigDecimal annualizedReturn;

    // ============ Risk Metrics ============

    /**
     * Maximum drawdown percentage
     */
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    /**
     * Sharpe ratio
     */
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    /**
     * Sortino ratio
     */
    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    /**
     * Calmar ratio
     */
    @Column(name = "calmar_ratio", precision = 10, scale = 4)
    private BigDecimal calmarRatio;

    /**
     * Annualized volatility
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal volatility;

    // ============ Trade Metrics ============

    @Column(name = "total_trades")
    private Integer totalTrades = 0;

    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Column(name = "losing_trades")
    private Integer losingTrades = 0;

    /**
     * Win rate percentage
     */
    @Column(name = "win_rate", precision = 10, scale = 4)
    private BigDecimal winRate;

    /**
     * Profit factor (gross profit / gross loss)
     */
    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    /**
     * Average winning trade amount
     */
    @Column(name = "average_win", precision = 20, scale = 8)
    private BigDecimal averageWin;

    /**
     * Average losing trade amount
     */
    @Column(name = "average_loss", precision = 20, scale = 8)
    private BigDecimal averageLoss;

    /**
     * Maximum consecutive losses
     */
    @Column(name = "max_consecutive_losses")
    private Integer maxConsecutiveLosses;

    // ============ Time Metrics ============

    /**
     * Average holding time in milliseconds
     */
    @Column(name = "average_holding_time")
    private Long averageHoldingTime;

    /**
     * Trading frequency (trades per day)
     */
    @Column(name = "trading_frequency", precision = 10, scale = 4)
    private BigDecimal tradingFrequency;

    // ============ Final State ============

    @Column(name = "final_equity", precision = 20, scale = 8)
    private BigDecimal finalEquity;

    @Column(name = "peak_equity", precision = 20, scale = 8)
    private BigDecimal peakEquity;

    // ============ Detailed Data (as JSON String) ============

    /**
     * Equity curve: JSON array of {timestamp, equity, drawdown}
     */
    @Column(name = "equity_curve", columnDefinition = "TEXT")
    private String equityCurve;

    /**
     * Monthly returns: JSON object of {YYYY-MM: return_percentage}
     */
    @Column(name = "monthly_returns", columnDefinition = "TEXT")
    private String monthlyReturns;

    /**
     * Trade records as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String trades;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
