package com.lucance.boot.backend.backtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucance.boot.backend.backtest.model.BacktestState;
import com.lucance.boot.backend.backtest.model.Order;
import com.lucance.boot.backend.backtest.model.Position;
import com.lucance.boot.backend.chan.ChanCalculationEngine;
import com.lucance.boot.backend.chan.model.TradingPoint;
import com.lucance.boot.backend.entity.BacktestResult;
import com.lucance.boot.backend.entity.BacktestTask;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.entity.Strategy;
import com.lucance.boot.backend.repository.KlineRepository;
import com.lucance.boot.backend.service.BacktestService;
import com.lucance.boot.backend.service.StrategyService;
import com.lucance.boot.backend.strategy.ConditionEvaluator;
import com.lucance.boot.backend.strategy.model.EntryConditionConfig;
import com.lucance.boot.backend.strategy.model.ExitConditionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 回测引擎
 * 负责执行回测任务，模拟交易，计算性能指标
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestEngine {

    private final KlineRepository klineRepository;
    private final StrategyService strategyService;
    private final BacktestService backtestService;
    private final ChanCalculationEngine chanEngine;
    private final ConditionEvaluator conditionEvaluator;
    private final MetricsCalculator metricsCalculator;
    private final ObjectMapper objectMapper;

    /**
     * 执行回测任务
     */
    public void executeBacktest(Long taskId) {
        log.info("Starting backtest execution for task {}", taskId);

        try {
            // Load task
            BacktestTask task = backtestService.getTask(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

            // Load strategy
            Strategy strategy = strategyService.getStrategyById(task.getStrategyId())
                    .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + task.getStrategyId()));

            // Parse strategy conditions
            EntryConditionConfig entryConfig = conditionEvaluator.parseEntryConditions(strategy.getEntryConditions());
            ExitConditionConfig exitConfig = conditionEvaluator.parseExitConditions(strategy.getExitConditions());

            if (entryConfig == null || exitConfig == null) {
                throw new IllegalArgumentException("Invalid strategy conditions");
            }

            // Execute backtest for each symbol
            String[] symbols = task.getSymbolsArray();
            String[] intervals = task.getIntervalsArray();

            if (symbols.length == 0 || intervals.length == 0) {
                throw new IllegalArgumentException("No symbols or intervals specified");
            }

            // For simplicity, use first symbol and interval
            // TODO: Support multiple symbols/intervals
            String symbol = symbols[0];
            String interval = intervals[0];

            // Run backtest
            BacktestResult result = runBacktest(task, strategy, entryConfig, exitConfig, symbol, interval);

            // Save result
            backtestService.saveResult(result);
            backtestService.markAsCompleted(taskId);

            log.info("Backtest completed for task {}: Total Return {}%, Max Drawdown {}%",
                    taskId, result.getTotalReturn(), result.getMaxDrawdown());

        } catch (Exception e) {
            log.error("Backtest failed for task {}: {}", taskId, e.getMessage(), e);
            backtestService.markAsFailed(taskId, e.getMessage());
        }
    }

    /**
     * 运行回测
     */
    private BacktestResult runBacktest(
            BacktestTask task,
            Strategy strategy,
            EntryConditionConfig entryConfig,
            ExitConditionConfig exitConfig,
            String symbol,
            String interval) {

        log.info("Running backtest: symbol={}, interval={}, period={} to {}",
                symbol, interval, task.getStartTime(), task.getEndTime());

        // Initialize backtest state
        BacktestState state = BacktestState.initialize(task.getId(), task.getInitialCapital());

        // Load K-lines for the backtest period
        List<Kline> klines = klineRepository.findBySymbolAndIntervalAndTimeRange(
                symbol,
                interval,
                task.getStartTime().toInstant(),
                task.getEndTime().toInstant()
        );

        if (klines.isEmpty()) {
            log.warn("No K-lines found for backtest period");
            return createEmptyResult(task);
        }

        log.info("Loaded {} K-lines for backtest", klines.size());

        // Process each K-line
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalKlines = klines.size();

        for (int i = 0; i < klines.size(); i++) {
            Kline currentKline = klines.get(i);

            // Update progress every 10%
            if (i % Math.max(1, totalKlines / 10) == 0) {
                int progress = (int) ((i * 100.0) / totalKlines);
                backtestService.updateTaskStatus(task.getId(), "running", progress, null);
            }

            // Get historical K-lines up to current point (for Chan analysis)
            List<Kline> historicalKlines = klines.subList(Math.max(0, i - 500), i + 1);

            // Calculate Chan analysis
            ChanCalculationEngine.ChanResultFull chanResult = chanEngine.calculateFull(historicalKlines);

            // Build evaluation context
            ConditionEvaluator.EvaluationContext context = new ConditionEvaluator.EvaluationContext(
                    currentKline.getClose(),
                    currentKline.getTime().toEpochMilli(),
                    chanResult.bis(),
                    chanResult.xianduans(),
                    chanResult.zhongshus(),
                    chanResult.tradingPoints(),
                    Collections.emptyList(), // divergences - not implemented yet
                    Collections.emptyList()  // MACD data - not implemented yet
            );

            // Update current price for all open positions
            for (Position position : state.getOpenPositions()) {
                position.setCurrentPrice(currentKline.getClose());
                position.updatePeakPrice();
            }

            // Check exit conditions for open positions
            checkExitConditions(state, context, exitConfig, task, currentKline);

            // Check entry conditions if we have available capital
            if (state.getOpenPositions().size() < getMaxPositions(strategy) &&
                    state.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
                checkEntryConditions(state, context, entryConfig, exitConfig, task, strategy, currentKline);
            }

            // Update equity
            BigDecimal totalPositionValue = state.getTotalPositionValue();
            BigDecimal currentEquity = state.getAvailableBalance().add(totalPositionValue);
            state.updateEquity(currentEquity, currentKline.getTime().toEpochMilli());

            processedCount.incrementAndGet();
        }

        // Close any remaining open positions at the end
        closeAllPositions(state, klines.get(klines.size() - 1), task, "Backtest ended");

        // Calculate final metrics
        long startTime = task.getStartTime().toInstant().toEpochMilli();
        long endTime = task.getEndTime().toInstant().toEpochMilli();
        MetricsCalculator.MetricsResult metrics = metricsCalculator.calculateMetrics(state, startTime, endTime);

        // Create result
        return createResult(task, state, metrics);
    }

    /**
     * 检查入场条件
     */
    private void checkEntryConditions(
            BacktestState state,
            ConditionEvaluator.EvaluationContext context,
            EntryConditionConfig entryConfig,
            ExitConditionConfig exitConfig,
            BacktestTask task,
            Strategy strategy,
            Kline currentKline) {

        ConditionEvaluator.EvaluationResult result = conditionEvaluator.evaluateEntry(entryConfig, context);

        if (result.satisfied()) {
            // Determine position side based on signal
            TradingPoint triggerPoint = result.triggerPoint();
            if (triggerPoint == null) {
                return;
            }

            Position.PositionSide side = triggerPoint.getType() == TradingPoint.PointType.BUY
                    ? Position.PositionSide.LONG
                    : Position.PositionSide.SHORT;

            // Calculate position size
            BigDecimal positionSize = calculatePositionSize(state, strategy, currentKline.getClose());

            if (positionSize.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }

            // Calculate stop loss and take profit
            BigDecimal stopLoss = calculateStopLoss(exitConfig, currentKline.getClose(), side, context);
            BigDecimal takeProfit = calculateTakeProfit(exitConfig, currentKline.getClose(), side, context);

            // Create and execute buy order
            Order order = createOrder(
                    state,
                    currentKline.getSymbol(),
                    side == Position.PositionSide.LONG ? Order.OrderSide.BUY : Order.OrderSide.SELL,
                    positionSize,
                    currentKline.getClose(),
                    task,
                    result.reason()
            );

            executeOrder(state, order, currentKline, task);

            // Create position
            Position position = Position.builder()
                    .id(UUID.randomUUID().toString())
                    .symbol(currentKline.getSymbol())
                    .side(side)
                    .quantity(positionSize)
                    .entryPrice(order.getFilledPrice())
                    .currentPrice(currentKline.getClose())
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .openTime(currentKline.getTime().toEpochMilli())
                    .status(Position.PositionStatus.OPEN)
                    .strategyId(strategy.getId())
                    .totalCommission(order.getCommission())
                    .peakPrice(currentKline.getClose())
                    .build();

            state.openPosition(position);

            log.debug("Opened {} position: {} @ {}, SL: {}, TP: {}",
                    side, positionSize, order.getFilledPrice(), stopLoss, takeProfit);
        }
    }

    /**
     * 检查出场条件
     */
    private void checkExitConditions(
            BacktestState state,
            ConditionEvaluator.EvaluationContext context,
            ExitConditionConfig exitConfig,
            BacktestTask task,
            Kline currentKline) {

        List<Position> positionsToClose = new ArrayList<>();

        for (Position position : state.getOpenPositions()) {
            String closeReason = null;

            // Check stop loss
            if (position.shouldStopLoss()) {
                closeReason = "Stop loss hit";
            }
            // Check take profit
            else if (position.shouldTakeProfit()) {
                closeReason = "Take profit hit";
            }
            // TODO: Check strategy exit conditions
            // This would require evaluating exit conditions from exitConfig

            if (closeReason != null) {
                positionsToClose.add(position);
                closePosition(state, position, currentKline, task, closeReason);
            }
        }
    }

    /**
     * 平仓
     */
    private void closePosition(
            BacktestState state,
            Position position,
            Kline currentKline,
            BacktestTask task,
            String reason) {

        // Create sell order
        Order order = createOrder(
                state,
                position.getSymbol(),
                position.getSide() == Position.PositionSide.LONG ? Order.OrderSide.SELL : Order.OrderSide.BUY,
                position.getQuantity(),
                currentKline.getClose(),
                task,
                reason
        );

        executeOrder(state, order, currentKline, task);

        // Calculate realized PnL
        BigDecimal pnl = position.getUnrealizedPnl();
        BigDecimal totalCost = order.getCommission().add(position.getTotalCommission());
        position.setRealizedPnl(pnl.subtract(totalCost));
        position.setCloseTime(currentKline.getTime().toEpochMilli());

        state.closePosition(position);

        log.debug("Closed {} position: PnL = {}, Reason: {}",
                position.getSide(), position.getRealizedPnl(), reason);
    }

    /**
     * 平掉所有持仓
     */
    private void closeAllPositions(BacktestState state, Kline lastKline, BacktestTask task, String reason) {
        List<Position> openPositions = new ArrayList<>(state.getOpenPositions());
        for (Position position : openPositions) {
            position.setCurrentPrice(lastKline.getClose());
            closePosition(state, position, lastKline, task, reason);
        }
    }

    /**
     * 创建订单
     */
    private Order createOrder(
            BacktestState state,
            String symbol,
            Order.OrderSide side,
            BigDecimal quantity,
            BigDecimal price,
            BacktestTask task,
            String reason) {

        return Order.builder()
                .id(UUID.randomUUID().toString())
                .symbol(symbol)
                .type(Order.OrderType.MARKET)
                .side(side)
                .quantity(quantity)
                .price(price)
                .status(Order.OrderStatus.PENDING)
                .createTime(System.currentTimeMillis())
                .strategyId(task.getStrategyId())
                .triggerReason(reason)
                .build();
    }

    /**
     * 执行订单（模拟）
     */
    private void executeOrder(BacktestState state, Order order, Kline currentKline, BacktestTask task) {
        // Apply slippage
        BigDecimal slippageRate = task.getSlippage() != null ? task.getSlippage() : BigDecimal.ZERO;
        BigDecimal slippageAmount = order.getPrice().multiply(slippageRate);

        BigDecimal filledPrice = order.getSide() == Order.OrderSide.BUY
                ? order.getPrice().add(slippageAmount)
                : order.getPrice().subtract(slippageAmount);

        // Calculate commission
        BigDecimal commissionRate = task.getCommission() != null ? task.getCommission() : new BigDecimal("0.001");
        BigDecimal commissionAmount = filledPrice.multiply(order.getQuantity()).multiply(commissionRate);

        order.setFilledPrice(filledPrice);
        order.setFilledQuantity(order.getQuantity());
        order.setCommission(commissionAmount);
        order.setSlippage(slippageAmount.multiply(order.getQuantity()));
        order.setStatus(Order.OrderStatus.FILLED);
        order.setFillTime(currentKline.getTime().toEpochMilli());

        state.addOrder(order);
    }

    /**
     * 计算仓位大小
     */
    private BigDecimal calculatePositionSize(BacktestState state, Strategy strategy, BigDecimal currentPrice) {
        // Simple fixed percentage of available balance
        BigDecimal positionValue = state.getAvailableBalance().multiply(new BigDecimal("0.1")); // 10% per position
        return positionValue.divide(currentPrice, 8, RoundingMode.HALF_UP);
    }

    /**
     * 计算止损价格
     */
    private BigDecimal calculateStopLoss(
            ExitConditionConfig exitConfig,
            BigDecimal entryPrice,
            Position.PositionSide side,
            ConditionEvaluator.EvaluationContext context) {

        // Default 2% stop loss
        BigDecimal stopLossPercent = new BigDecimal("0.02");

        if (side == Position.PositionSide.LONG) {
            return entryPrice.multiply(BigDecimal.ONE.subtract(stopLossPercent));
        } else {
            return entryPrice.multiply(BigDecimal.ONE.add(stopLossPercent));
        }
    }

    /**
     * 计算止盈价格
     */
    private BigDecimal calculateTakeProfit(
            ExitConditionConfig exitConfig,
            BigDecimal entryPrice,
            Position.PositionSide side,
            ConditionEvaluator.EvaluationContext context) {

        // Default 4% take profit (2:1 risk-reward)
        BigDecimal takeProfitPercent = new BigDecimal("0.04");

        if (side == Position.PositionSide.LONG) {
            return entryPrice.multiply(BigDecimal.ONE.add(takeProfitPercent));
        } else {
            return entryPrice.multiply(BigDecimal.ONE.subtract(takeProfitPercent));
        }
    }

    /**
     * 获取最大持仓数
     */
    private int getMaxPositions(Strategy strategy) {
        // TODO: Parse from strategy risk control
        return 3; // Default max 3 positions
    }

    /**
     * 创建回测结果
     */
    private BacktestResult createResult(BacktestTask task, BacktestState state, MetricsCalculator.MetricsResult metrics) {
        BacktestResult result = new BacktestResult();
        result.setTaskId(task.getId());

        // Return metrics
        result.setTotalReturn(metrics.totalReturn());
        result.setAnnualizedReturn(metrics.annualizedReturn());

        // Risk metrics
        result.setMaxDrawdown(metrics.maxDrawdown());
        result.setSharpeRatio(metrics.sharpeRatio());
        result.setSortinoRatio(metrics.sortinoRatio());
        result.setCalmarRatio(metrics.calmarRatio());
        result.setVolatility(metrics.volatility());

        // Trade metrics
        result.setTotalTrades(metrics.totalTrades());
        result.setWinningTrades(metrics.winningTrades());
        result.setLosingTrades(metrics.losingTrades());
        result.setWinRate(metrics.winRate());
        result.setProfitFactor(metrics.profitFactor());
        result.setAverageWin(metrics.averageWin());
        result.setAverageLoss(metrics.averageLoss());
        result.setMaxConsecutiveLosses(metrics.maxConsecutiveLosses());

        // Time metrics
        result.setAverageHoldingTime(metrics.averageHoldingTime());
        result.setTradingFrequency(metrics.tradingFrequency());

        // Final state
        result.setFinalEquity(state.getEquity());
        result.setPeakEquity(state.getPeakEquity());

        // Serialize detailed data to JSON
        try {
            result.setEquityCurve(objectMapper.writeValueAsString(state.getEquityCurve()));
            result.setTrades(objectMapper.writeValueAsString(state.getClosedPositions()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize backtest data", e);
        }

        return result;
    }

    /**
     * 创建空结果（无数据时）
     */
    private BacktestResult createEmptyResult(BacktestTask task) {
        BacktestResult result = new BacktestResult();
        result.setTaskId(task.getId());
        result.setTotalReturn(BigDecimal.ZERO);
        result.setTotalTrades(0);
        return result;
    }
}
