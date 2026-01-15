package com.lucance.boot.backend.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucance.boot.backend.chan.model.*;
import com.lucance.boot.backend.strategy.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 条件评估器
 * 负责解析和评估策略条件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * 评估上下文，包含当前所有缠论分析结果
     */
    public record EvaluationContext(
            BigDecimal currentPrice,
            long currentTime,
            List<Bi> bis,
            List<Xianduan> xianduans,
            List<Zhongshu> zhongshus,
            List<TradingPoint> tradingPoints,
            List<DivergenceResult> divergences,
            List<MACDResult> macdData) {
    }

    /**
     * 评估结果
     */
    public record EvaluationResult(
            boolean satisfied,
            String reason,
            Condition matchedCondition,
            TradingPoint triggerPoint) {
        public static EvaluationResult success(String reason, Condition condition, TradingPoint point) {
            return new EvaluationResult(true, reason, condition, point);
        }

        public static EvaluationResult failure(String reason) {
            return new EvaluationResult(false, reason, null, null);
        }
    }

    /**
     * 解析入场条件JSON
     */
    public EntryConditionConfig parseEntryConditions(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, EntryConditionConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse entry conditions: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析出场条件JSON
     */
    public ExitConditionConfig parseExitConditions(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ExitConditionConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse exit conditions: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 评估入场条件
     */
    public EvaluationResult evaluateEntry(EntryConditionConfig config, EvaluationContext context) {
        if (config == null || !config.isValid()) {
            return EvaluationResult.failure("Invalid entry condition config");
        }

        // 评估主要条件
        EvaluationResult primaryResult = evaluateConditionGroup(config.getPrimary(), context);
        if (!primaryResult.satisfied()) {
            return EvaluationResult.failure("Primary condition not satisfied: " + primaryResult.reason());
        }

        // 如果需要多级别共振
        if (Boolean.TRUE.equals(config.getRequireResonance())) {
            // 评估次要条件
            if (config.getSecondary() != null) {
                EvaluationResult secondaryResult = evaluateConditionGroup(config.getSecondary(), context);
                if (!secondaryResult.satisfied()) {
                    return EvaluationResult.failure("Secondary condition not satisfied for resonance");
                }
            }

            // 评估第三级条件
            if (config.getTertiary() != null) {
                EvaluationResult tertiaryResult = evaluateConditionGroup(config.getTertiary(), context);
                if (!tertiaryResult.satisfied()) {
                    return EvaluationResult.failure("Tertiary condition not satisfied for resonance");
                }
            }
        }

        return primaryResult;
    }

    /**
     * 评估条件组
     */
    public EvaluationResult evaluateConditionGroup(ConditionGroup group, EvaluationContext context) {
        if (group == null || group.getConditions() == null || group.getConditions().isEmpty()) {
            return EvaluationResult.failure("Empty condition group");
        }

        boolean isAndLogic = group.getLogic() == ConditionGroup.Logic.AND;
        TradingPoint firstMatchedPoint = null;
        Condition firstMatchedCondition = null;
        StringBuilder reasons = new StringBuilder();

        for (Condition condition : group.getConditions()) {
            EvaluationResult result = evaluateCondition(condition, context);

            if (result.satisfied()) {
                if (firstMatchedPoint == null && result.triggerPoint() != null) {
                    firstMatchedPoint = result.triggerPoint();
                    firstMatchedCondition = result.matchedCondition();
                }

                if (!isAndLogic) {
                    // OR逻辑，任一满足即可
                    return EvaluationResult.success(result.reason(), condition, result.triggerPoint());
                }
            } else {
                if (isAndLogic) {
                    // AND逻辑，任一不满足则失败
                    return EvaluationResult.failure(result.reason());
                }
                reasons.append(result.reason()).append("; ");
            }
        }

        if (isAndLogic) {
            // 所有条件都满足
            return EvaluationResult.success("All conditions satisfied", firstMatchedCondition, firstMatchedPoint);
        } else {
            // 所有OR条件都不满足
            return EvaluationResult.failure("No condition satisfied: " + reasons);
        }
    }

    /**
     * 评估单个条件
     */
    public EvaluationResult evaluateCondition(Condition condition, EvaluationContext context) {
        if (condition == null) {
            return EvaluationResult.failure("Null condition");
        }

        EvaluationResult result = switch (condition) {
            case TradingPointCondition tpc -> evaluateTradingPointCondition(tpc, context);
            case TrendCondition tc -> evaluateTrendCondition(tc, context);
            case DivergenceCondition dc -> evaluateDivergenceCondition(dc, context);
            case ZhongshuCondition zc -> evaluateZhongshuCondition(zc, context);
            case MACDCondition mc -> evaluateMACDCondition(mc, context);
            case PriceCondition pc -> evaluatePriceCondition(pc, context);
            default -> EvaluationResult.failure("Unknown condition type: " + condition.getClass().getSimpleName());
        };

        // 处理取反
        if (condition.isNegated()) {
            if (result.satisfied()) {
                return EvaluationResult.failure("Negated condition was satisfied");
            } else {
                return EvaluationResult.success("Negated condition not satisfied (expected)", null, null);
            }
        }

        return result;
    }

    /**
     * 评估买卖点条件
     */
    private EvaluationResult evaluateTradingPointCondition(TradingPointCondition condition, EvaluationContext context) {
        if (context.tradingPoints() == null || context.tradingPoints().isEmpty()) {
            return EvaluationResult.failure("No trading points available");
        }

        // 检查最近的买卖点
        for (int i = context.tradingPoints().size() - 1; i >= 0; i--) {
            TradingPoint point = context.tradingPoints().get(i);

            // 只检查最近5分钟内的买卖点
            if (context.currentTime() - point.getTimestamp() > 300000) {
                break;
            }

            if (condition.matches(point)) {
                return EvaluationResult.success(
                        "Found matching trading point: " + point.getSignalDescription(),
                        condition,
                        point);
            }
        }

        return EvaluationResult.failure("No matching trading point found");
    }

    /**
     * 评估趋势条件
     */
    private EvaluationResult evaluateTrendCondition(TrendCondition condition, EvaluationContext context) {
        if (context.xianduans() == null || context.xianduans().isEmpty()) {
            return EvaluationResult.failure("No xianduans available for trend analysis");
        }

        if (condition.matches(context.xianduans())) {
            return EvaluationResult.success(
                    "Trend condition satisfied: " + condition.toHumanReadable(),
                    condition,
                    null);
        }

        return EvaluationResult.failure("Trend condition not satisfied");
    }

    /**
     * 评估背驰条件
     */
    private EvaluationResult evaluateDivergenceCondition(DivergenceCondition condition, EvaluationContext context) {
        if (context.divergences() == null || context.divergences().isEmpty()) {
            return EvaluationResult.failure("No divergences available");
        }

        // 检查最近的背驰
        for (int i = context.divergences().size() - 1; i >= 0; i--) {
            DivergenceResult divergence = context.divergences().get(i);

            if (condition.matches(divergence)) {
                return EvaluationResult.success(
                        "Found matching divergence: " + condition.toHumanReadable(),
                        condition,
                        null);
            }
        }

        return EvaluationResult.failure("No matching divergence found");
    }

    /**
     * 评估中枢条件
     */
    private EvaluationResult evaluateZhongshuCondition(ZhongshuCondition condition, EvaluationContext context) {
        if (context.zhongshus() == null || context.zhongshus().isEmpty()) {
            return EvaluationResult.failure("No zhongshus available");
        }

        if (condition.matches(context.currentPrice(), context.zhongshus())) {
            return EvaluationResult.success(
                    "Zhongshu condition satisfied: " + condition.toHumanReadable(),
                    condition,
                    null);
        }

        return EvaluationResult.failure("Zhongshu condition not satisfied");
    }

    /**
     * 评估MACD条件
     */
    private EvaluationResult evaluateMACDCondition(MACDCondition condition, EvaluationContext context) {
        if (context.macdData() == null || context.macdData().isEmpty()) {
            return EvaluationResult.failure("No MACD data available");
        }

        int size = context.macdData().size();
        MACDResult current = context.macdData().get(size - 1);
        MACDResult previous = size > 1 ? context.macdData().get(size - 2) : null;

        if (condition.matches(current, previous)) {
            return EvaluationResult.success(
                    "MACD condition satisfied: " + condition.toHumanReadable(),
                    condition,
                    null);
        }

        return EvaluationResult.failure("MACD condition not satisfied");
    }

    /**
     * 评估价格条件
     */
    private EvaluationResult evaluatePriceCondition(PriceCondition condition, EvaluationContext context) {
        if (context.currentPrice() == null) {
            return EvaluationResult.failure("Current price not available");
        }

        // 根据参考类型获取参考价格
        BigDecimal referencePrice = getReferencePrice(condition, context);
        if (referencePrice == null) {
            return EvaluationResult.failure("Reference price not available");
        }

        if (condition.matches(context.currentPrice(), referencePrice)) {
            return EvaluationResult.success(
                    "Price condition satisfied: " + condition.toHumanReadable(),
                    condition,
                    null);
        }

        return EvaluationResult.failure("Price condition not satisfied");
    }

    /**
     * 获取参考价格
     */
    private BigDecimal getReferencePrice(PriceCondition condition, EvaluationContext context) {
        String refType = condition.getReferenceType();

        return switch (refType) {
            case "fixed" -> condition.getValue();
            case "zhongshu_high" -> {
                if (context.zhongshus() == null || context.zhongshus().isEmpty()) {
                    yield null;
                }
                yield context.zhongshus().get(context.zhongshus().size() - 1).getHigh();
            }
            case "zhongshu_low" -> {
                if (context.zhongshus() == null || context.zhongshus().isEmpty()) {
                    yield null;
                }
                yield context.zhongshus().get(context.zhongshus().size() - 1).getLow();
            }
            case "fenxing" -> {
                // 获取最近分型价格（从买卖点）
                if (context.tradingPoints() == null || context.tradingPoints().isEmpty()) {
                    yield null;
                }
                yield context.tradingPoints().get(context.tradingPoints().size() - 1).getPrice();
            }
            default -> null;
        };
    }
}
