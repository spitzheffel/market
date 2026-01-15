package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 买卖点
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingPoint {

    /**
     * 买卖点唯一标识
     */
    private String id;

    /**
     * 买卖点类型
     */
    private PointType type;

    /**
     * 买卖点级别 (1/2/3)
     * 1类: 趋势转折点，需要背驰判断，最强信号
     * 2类: 回抽确认点，突破后的回测，次强信号
     * 3类: 中枢震荡点，中枢边缘机会，风险较高
     */
    private int level;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 置信度
     */
    private Confidence confidence;

    /**
     * 判定理由
     */
    private String reason;

    /**
     * 背驰数据（1类买卖点需要）
     */
    private DivergenceResult divergence;

    /**
     * 相关笔
     */
    private Bi relatedBi;

    /**
     * 相关线段
     */
    private Xianduan relatedXianduan;

    /**
     * 相关中枢
     */
    private Zhongshu relatedZhongshu;

    /**
     * 买卖点类型枚举
     */
    public enum PointType {
        BUY, // 买点
        SELL // 卖点
    }

    /**
     * 置信度枚举
     */
    public enum Confidence {
        HIGH, // 高置信度
        MEDIUM, // 中等置信度
        LOW // 低置信度
    }

    /**
     * 判断是否为强信号
     */
    public boolean isStrongSignal() {
        return level == 1 && confidence == Confidence.HIGH;
    }

    /**
     * 判断是否为中等信号
     */
    public boolean isMediumSignal() {
        return level == 2 || (level == 1 && confidence == Confidence.MEDIUM);
    }

    /**
     * 获取信号等级描述
     */
    public String getSignalDescription() {
        String typeStr = type == PointType.BUY ? "买" : "卖";
        return level + "类" + typeStr + "点";
    }
}
