package com.lucance.boot.backend.strategy.model;

/**
 * 策略条件类型枚举
 * 定义缠论分析中可用的条件类型
 */
public enum ConditionType {

    /**
     * 买卖点条件
     * 匹配1类/2类/3类买卖点
     */
    TRADING_POINT("trading-point", "买卖点"),

    /**
     * 趋势条件
     * 检查是否处于上涨/下跌趋势
     */
    TREND("trend", "趋势"),

    /**
     * 背驰条件
     * 检查MACD背驰信号
     */
    DIVERGENCE("divergence", "背驰"),

    /**
     * 中枢条件
     * 检查价格与中枢的关系
     */
    ZHONGSHU("zhongshu", "中枢"),

    /**
     * 线段条件
     * 检查线段数量、方向等
     */
    XIANDUAN("xianduan", "线段"),

    /**
     * 笔条件
     * 检查笔的特征
     */
    BI("bi", "笔"),

    /**
     * MACD条件
     * 检查MACD指标状态
     */
    MACD("macd", "MACD指标"),

    /**
     * 价格条件
     * 检查价格水平或突破
     */
    PRICE("price", "价格");

    private final String code;
    private final String displayName;

    ConditionType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 从代码获取枚举
     */
    public static ConditionType fromCode(String code) {
        for (ConditionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown condition type: " + code);
    }
}
