package com.lucance.boot.backend.service;

import com.lucance.boot.backend.chan.ChanCalculationEngine;
import com.lucance.boot.backend.chan.model.TradingPoint;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.entity.Signal;
import com.lucance.boot.backend.entity.Strategy;
import com.lucance.boot.backend.repository.KlineRepository;
import com.lucance.boot.backend.strategy.ConditionEvaluator;
import com.lucance.boot.backend.strategy.model.EntryConditionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 信号生成服务
 * 基于策略条件和缠论分析结果，自动生成交易信号
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalGeneratorService {

    private final StrategyService strategyService;
    private final SignalService signalService;
    private final KlineRepository klineRepository;
    private final ChanCalculationEngine chanEngine;
    private final ConditionEvaluator conditionEvaluator;

    /**
     * 定时任务：每分钟检查一次信号
     * 可以通过配置文件调整频率
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000) // Every 60 seconds
    @Transactional
    public void generateSignals() {
        try {
            // Get all active strategies
            List<Strategy> activeStrategies = strategyService.getActiveStrategies();

            if (activeStrategies.isEmpty()) {
                return;
            }

            log.debug("Checking signals for {} active strategies", activeStrategies.size());

            // For each strategy, check if conditions are met
            for (Strategy strategy : activeStrategies) {
                try {
                    checkStrategySignals(strategy);
                } catch (Exception e) {
                    log.error("Error checking signals for strategy {}: {}", strategy.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in signal generation task: {}", e.getMessage(), e);
        }
    }

    /**
     * 为指定策略检查信号
     */
    private void checkStrategySignals(Strategy strategy) {
        // Parse strategy conditions
        EntryConditionConfig entryConfig = conditionEvaluator.parseEntryConditions(strategy.getEntryConditions());
        if (entryConfig == null) {
            log.warn("Invalid entry conditions for strategy {}", strategy.getId());
            return;
        }

        // Get applicable levels
        String[] levels = strategy.getLevelsArray();
        if (levels.length == 0) {
            return;
        }

        // Check signals for each level
        // For now, we'll check common symbols - this could be configurable
        String[] symbols = {"BTCUSDT", "ETHUSDT"}; // TODO: Make this configurable

        for (String symbol : symbols) {
            for (String level : levels) {
                try {
                    checkSignalForSymbolAndLevel(strategy, entryConfig, symbol, level);
                } catch (Exception e) {
                    log.error("Error checking signal for {}/{}: {}", symbol, level, e.getMessage());
                }
            }
        }
    }

    /**
     * 检查特定交易对和级别的信号
     */
    private void checkSignalForSymbolAndLevel(
            Strategy strategy,
            EntryConditionConfig entryConfig,
            String symbol,
            String level) {

        // Load recent K-lines (last 500 for Chan analysis)
        OffsetDateTime endTime = OffsetDateTime.now();
        OffsetDateTime startTime = endTime.minusHours(24); // Last 24 hours

        List<Kline> klines = klineRepository.findBySymbolAndIntervalAndTimeRange(
                symbol,
                level,
                startTime.toInstant(),
                endTime.toInstant()
        );

        if (klines.isEmpty()) {
            return;
        }

        // Limit to last 500 K-lines for performance
        if (klines.size() > 500) {
            klines = klines.subList(klines.size() - 500, klines.size());
        }

        // Calculate Chan analysis
        ChanCalculationEngine.ChanResultFull chanResult = chanEngine.calculateFull(klines);

        // Get current K-line
        Kline currentKline = klines.get(klines.size() - 1);

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

        // Evaluate entry conditions
        ConditionEvaluator.EvaluationResult result = conditionEvaluator.evaluateEntry(entryConfig, context);

        if (result.satisfied()) {
            // Check if we already have a recent pending signal for this symbol/strategy
            List<Signal> recentSignals = signalService.getPendingSignalsForSymbol(symbol);
            boolean alreadyExists = recentSignals.stream()
                    .anyMatch(s -> s.getStrategyId() != null &&
                            s.getStrategyId().equals(strategy.getId()) &&
                            s.getInterval().equals(level));

            if (alreadyExists) {
                log.debug("Signal already exists for strategy {} on {}/{}", strategy.getId(), symbol, level);
                return;
            }

            // Create new signal
            TradingPoint triggerPoint = result.triggerPoint();
            if (triggerPoint != null) {
                Signal signal = createSignal(strategy, symbol, level, triggerPoint, currentKline, result.reason());
                signalService.createSignal(signal);

                log.info("Generated signal: {} {} for {} on {}/{} - {}",
                        signal.getSignalType(),
                        signal.getLevel(),
                        symbol,
                        level,
                        strategy.getName(),
                        signal.getReason());
            }
        }
    }

    /**
     * 创建信号对象
     */
    private Signal createSignal(
            Strategy strategy,
            String symbol,
            String interval,
            TradingPoint tradingPoint,
            Kline currentKline,
            String reason) {

        Signal signal = new Signal();
        signal.setStrategyId(strategy.getId());
        signal.setSymbol(symbol);
        signal.setInterval(interval);
        signal.setSignalType(tradingPoint.getType() == TradingPoint.PointType.BUY ? "buy" : "sell");
        signal.setLevel(tradingPoint.getLevel());
        signal.setEntryPrice(currentKline.getClose());

        // Calculate stop loss and take profit (simple 2% / 4% for now)
        BigDecimal stopLossPercent = new BigDecimal("0.02");
        BigDecimal takeProfitPercent = new BigDecimal("0.04");

        if ("buy".equals(signal.getSignalType())) {
            signal.setStopLoss(currentKline.getClose().multiply(BigDecimal.ONE.subtract(stopLossPercent)));
            signal.setTakeProfit(currentKline.getClose().multiply(BigDecimal.ONE.add(takeProfitPercent)));
        } else {
            signal.setStopLoss(currentKline.getClose().multiply(BigDecimal.ONE.add(stopLossPercent)));
            signal.setTakeProfit(currentKline.getClose().multiply(BigDecimal.ONE.subtract(takeProfitPercent)));
        }

        // Set confidence based on trading point confidence
        signal.setConfidence(mapConfidence(tradingPoint.getConfidence()));
        signal.setReason(reason != null ? reason : tradingPoint.getReason());

        // Set related Chan structure IDs (if available)
        // Note: Chan structure IDs are String type, but Signal entity expects Long
        // We skip setting these IDs for now to avoid conversion issues
        // TODO: Consider changing Signal entity to use String IDs or add conversion logic

        signal.setStatus("pending");

        return signal;
    }

    /**
     * 映射置信度
     */
    private String mapConfidence(TradingPoint.Confidence confidence) {
        if (confidence == null) {
            return "medium";
        }
        return switch (confidence) {
            case HIGH -> "high";
            case MEDIUM -> "medium";
            case LOW -> "low";
        };
    }

    /**
     * 手动为指定交易对生成信号
     */
    @Transactional
    public void generateSignalsForSymbol(String symbol, String interval) {
        log.info("Manually generating signals for {}/{}", symbol, interval);

        List<Strategy> activeStrategies = strategyService.getActiveStrategies();

        for (Strategy strategy : activeStrategies) {
            // Check if strategy applies to this interval
            boolean appliesToInterval = false;
            for (String level : strategy.getLevelsArray()) {
                if (level.equals(interval)) {
                    appliesToInterval = true;
                    break;
                }
            }

            if (!appliesToInterval) {
                continue;
            }

            EntryConditionConfig entryConfig = conditionEvaluator.parseEntryConditions(strategy.getEntryConditions());
            if (entryConfig != null) {
                checkSignalForSymbolAndLevel(strategy, entryConfig, symbol, interval);
            }
        }
    }

    /**
     * 清理过期信号
     * 每小时运行一次，将超过24小时的pending信号标记为expired
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000) // Every hour
    @Transactional
    public void expireOldSignals() {
        try {
            List<Signal> pendingSignals = signalService.getPendingSignals();
            OffsetDateTime expiryThreshold = OffsetDateTime.now().minusHours(24);

            int expiredCount = 0;
            for (Signal signal : pendingSignals) {
                if (signal.getCreatedAt().isBefore(expiryThreshold)) {
                    signalService.updateSignalStatus(signal.getId(), "expired");
                    expiredCount++;
                }
            }

            if (expiredCount > 0) {
                log.info("Expired {} old signals", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error expiring old signals: {}", e.getMessage(), e);
        }
    }
}
