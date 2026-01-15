package com.lucance.boot.backend.backtest;

import com.lucance.boot.backend.backtest.model.BacktestState;
import com.lucance.boot.backend.backtest.model.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 回测性能指标计算器
 * 计算各种回测性能指标，包括收益、风险、交易统计等
 */
@Slf4j
@Component
public class MetricsCalculator {

    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
    private static final BigDecimal TRADING_DAYS_PER_YEAR = new BigDecimal("252");
    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03"); // 3% annual risk-free rate

    /**
     * 计算结果封装类
     */
    public record MetricsResult(
            // Return metrics
            BigDecimal totalReturn,
            BigDecimal annualizedReturn,

            // Risk metrics
            BigDecimal maxDrawdown,
            BigDecimal sharpeRatio,
            BigDecimal sortinoRatio,
            BigDecimal calmarRatio,
            BigDecimal volatility,

            // Trade metrics
            int totalTrades,
            int winningTrades,
            int losingTrades,
            BigDecimal winRate,
            BigDecimal profitFactor,
            BigDecimal averageWin,
            BigDecimal averageLoss,
            int maxConsecutiveLosses,

            // Time metrics
            long averageHoldingTime,
            BigDecimal tradingFrequency
    ) {
    }

    /**
     * 计算所有性能指标
     */
    public MetricsResult calculateMetrics(BacktestState state, long startTime, long endTime) {
        if (state == null) {
            throw new IllegalArgumentException("BacktestState cannot be null");
        }

        // Calculate return metrics
        BigDecimal totalReturn = calculateTotalReturn(state);
        BigDecimal annualizedReturn = calculateAnnualizedReturn(totalReturn, startTime, endTime);

        // Calculate risk metrics
        BigDecimal maxDrawdown = state.getMaxDrawdown();
        BigDecimal volatility = calculateVolatility(state);
        BigDecimal sharpeRatio = calculateSharpeRatio(annualizedReturn, volatility);
        BigDecimal sortinoRatio = calculateSortinoRatio(annualizedReturn, state);
        BigDecimal calmarRatio = calculateCalmarRatio(annualizedReturn, maxDrawdown);

        // Calculate trade metrics
        int totalTrades = state.getClosedPositions().size();
        int winningTrades = state.getWinningTrades();
        int losingTrades = state.getLosingTrades();
        BigDecimal winRate = calculateWinRate(winningTrades, totalTrades);
        BigDecimal profitFactor = calculateProfitFactor(state);
        BigDecimal averageWin = calculateAverageWin(state);
        BigDecimal averageLoss = calculateAverageLoss(state);
        int maxConsecutiveLosses = calculateMaxConsecutiveLosses(state);

        // Calculate time metrics
        long averageHoldingTime = calculateAverageHoldingTime(state);
        BigDecimal tradingFrequency = calculateTradingFrequency(totalTrades, startTime, endTime);

        return new MetricsResult(
                totalReturn, annualizedReturn,
                maxDrawdown, sharpeRatio, sortinoRatio, calmarRatio, volatility,
                totalTrades, winningTrades, losingTrades, winRate, profitFactor,
                averageWin, averageLoss, maxConsecutiveLosses,
                averageHoldingTime, tradingFrequency
        );
    }

    /**
     * 计算总收益率
     */
    private BigDecimal calculateTotalReturn(BacktestState state) {
        if (state.getInitialCapital().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal profit = state.getEquity().subtract(state.getInitialCapital());
        return profit.divide(state.getInitialCapital(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 计算年化收益率
     */
    private BigDecimal calculateAnnualizedReturn(BigDecimal totalReturn, long startTime, long endTime) {
        long durationMs = endTime - startTime;
        if (durationMs <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal durationDays = new BigDecimal(durationMs)
                .divide(new BigDecimal("86400000"), 4, RoundingMode.HALF_UP); // ms to days

        if (durationDays.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalReturn.multiply(DAYS_PER_YEAR)
                .divide(durationDays, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算波动率（年化）
     */
    private BigDecimal calculateVolatility(BacktestState state) {
        List<BigDecimal> returns = calculateDailyReturns(state);
        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(returns.size() - 1), 6, RoundingMode.HALF_UP);

        BigDecimal stdDev = sqrt(variance);

        // Annualize: daily volatility * sqrt(252)
        return stdDev.multiply(sqrt(TRADING_DAYS_PER_YEAR));
    }

    /**
     * 计算夏普比率
     */
    private BigDecimal calculateSharpeRatio(BigDecimal annualizedReturn, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = annualizedReturn.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                .subtract(RISK_FREE_RATE);

        return excessReturn.divide(volatility, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算索提诺比率
     */
    private BigDecimal calculateSortinoRatio(BigDecimal annualizedReturn, BacktestState state) {
        List<BigDecimal> returns = calculateDailyReturns(state);
        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Calculate downside deviation (only negative returns)
        List<BigDecimal> negativeReturns = returns.stream()
                .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());

        if (negativeReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal downsideVariance = negativeReturns.stream()
                .map(r -> r.pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(negativeReturns.size()), 6, RoundingMode.HALF_UP);

        BigDecimal downsideDeviation = sqrt(downsideVariance);
        BigDecimal annualizedDownsideDeviation = downsideDeviation.multiply(sqrt(TRADING_DAYS_PER_YEAR));

        if (annualizedDownsideDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = annualizedReturn.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                .subtract(RISK_FREE_RATE);

        return excessReturn.divide(annualizedDownsideDeviation, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算卡玛比率
     */
    private BigDecimal calculateCalmarRatio(BigDecimal annualizedReturn, BigDecimal maxDrawdown) {
        if (maxDrawdown.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return annualizedReturn.divide(maxDrawdown, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算胜率
     */
    private BigDecimal calculateWinRate(int winningTrades, int totalTrades) {
        if (totalTrades == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(winningTrades)
                .divide(new BigDecimal(totalTrades), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 计算盈亏比
     */
    private BigDecimal calculateProfitFactor(BacktestState state) {
        BigDecimal grossProfit = state.getClosedPositions().stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .map(Position::getRealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossLoss = state.getClosedPositions().stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0)
                .map(Position::getRealizedPnl)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
            return grossProfit.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("999.99") : BigDecimal.ZERO;
        }

        return grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算平均盈利
     */
    private BigDecimal calculateAverageWin(BacktestState state) {
        List<BigDecimal> wins = state.getClosedPositions().stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .map(Position::getRealizedPnl)
                .collect(Collectors.toList());

        if (wins.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = wins.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(wins.size()), 8, RoundingMode.HALF_UP);
    }

    /**
     * 计算平均亏损
     */
    private BigDecimal calculateAverageLoss(BacktestState state) {
        List<BigDecimal> losses = state.getClosedPositions().stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0)
                .map(Position::getRealizedPnl)
                .collect(Collectors.toList());

        if (losses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = losses.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(losses.size()), 8, RoundingMode.HALF_UP);
    }

    /**
     * 计算最大连续亏损次数
     */
    private int calculateMaxConsecutiveLosses(BacktestState state) {
        int maxConsecutive = 0;
        int currentConsecutive = 0;

        for (Position position : state.getClosedPositions()) {
            if (position.getRealizedPnl() != null && position.getRealizedPnl().compareTo(BigDecimal.ZERO) < 0) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 0;
            }
        }

        return maxConsecutive;
    }

    /**
     * 计算平均持仓时间
     */
    private long calculateAverageHoldingTime(BacktestState state) {
        if (state.getClosedPositions().isEmpty()) {
            return 0;
        }

        long totalHoldingTime = state.getClosedPositions().stream()
                .mapToLong(p -> p.getCloseTime() - p.getOpenTime())
                .sum();

        return totalHoldingTime / state.getClosedPositions().size();
    }

    /**
     * 计算交易频率（每天交易次数）
     */
    private BigDecimal calculateTradingFrequency(int totalTrades, long startTime, long endTime) {
        long durationMs = endTime - startTime;
        if (durationMs <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal durationDays = new BigDecimal(durationMs)
                .divide(new BigDecimal("86400000"), 4, RoundingMode.HALF_UP);

        if (durationDays.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(totalTrades)
                .divide(durationDays, 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算每日收益率
     */
    private List<BigDecimal> calculateDailyReturns(BacktestState state) {
        List<BigDecimal> dailyReturns = new ArrayList<>();
        List<BacktestState.EquityPoint> equityCurve = state.getEquityCurve();

        if (equityCurve.size() < 2) {
            return dailyReturns;
        }

        // Group by day and calculate daily returns
        long currentDay = equityCurve.get(0).getTimestamp() / 86400000;
        BigDecimal dayStartEquity = equityCurve.get(0).getEquity();

        for (int i = 1; i < equityCurve.size(); i++) {
            BacktestState.EquityPoint point = equityCurve.get(i);
            long pointDay = point.getTimestamp() / 86400000;

            if (pointDay != currentDay) {
                // New day, calculate return
                BigDecimal dayEndEquity = equityCurve.get(i - 1).getEquity();
                if (dayStartEquity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal dailyReturn = dayEndEquity.subtract(dayStartEquity)
                            .divide(dayStartEquity, 6, RoundingMode.HALF_UP);
                    dailyReturns.add(dailyReturn);
                }

                currentDay = pointDay;
                dayStartEquity = point.getEquity();
            }
        }

        // Add last day
        BigDecimal lastEquity = equityCurve.get(equityCurve.size() - 1).getEquity();
        if (dayStartEquity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyReturn = lastEquity.subtract(dayStartEquity)
                    .divide(dayStartEquity, 6, RoundingMode.HALF_UP);
            dailyReturns.add(dailyReturn);
        }

        return dailyReturns;
    }

    /**
     * 计算平方根（使用牛顿迭代法）
     */
    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal x = value;
        BigDecimal two = new BigDecimal("2");
        BigDecimal epsilon = new BigDecimal("0.0000001");

        for (int i = 0; i < 50; i++) {
            BigDecimal nextX = x.add(value.divide(x, 10, RoundingMode.HALF_UP))
                    .divide(two, 10, RoundingMode.HALF_UP);

            if (x.subtract(nextX).abs().compareTo(epsilon) < 0) {
                break;
            }
            x = nextX;
        }

        return x.setScale(4, RoundingMode.HALF_UP);
    }
}
