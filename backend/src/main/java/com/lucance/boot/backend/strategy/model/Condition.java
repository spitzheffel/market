package com.lucance.boot.backend.strategy.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 策略条件基类
 * 所有具体条件类型都继承此类
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TradingPointCondition.class, name = "trading-point"),
        @JsonSubTypes.Type(value = TrendCondition.class, name = "trend"),
        @JsonSubTypes.Type(value = DivergenceCondition.class, name = "divergence"),
        @JsonSubTypes.Type(value = ZhongshuCondition.class, name = "zhongshu"),
        @JsonSubTypes.Type(value = MACDCondition.class, name = "macd"),
        @JsonSubTypes.Type(value = PriceCondition.class, name = "price")
})
public abstract class Condition {

    /**
     * 条件类型
     */
    protected ConditionType type;

    /**
     * 条件是否取反
     */
    protected boolean negated = false;

    /**
     * 条件描述（用于日志和用户展示）
     */
    protected String description;

    /**
     * 获取条件类型代码
     */
    public abstract String getTypeCode();

    /**
     * 验证条件参数是否有效
     */
    public abstract boolean isValid();

    /**
     * 生成人类可读的条件描述
     */
    public abstract String toHumanReadable();
}
