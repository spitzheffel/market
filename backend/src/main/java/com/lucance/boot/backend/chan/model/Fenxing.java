package com.lucance.boot.backend.chan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 分型
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fenxing {

    private String id;
    private FenxingType type;
    private int centerIndex;
    private int leftIndex;
    private int rightIndex;
    private BigDecimal price;
    private Instant time;
    private List<MergedKline> klines;
    private boolean confirmed;

    /**
     * 分型类型
     */
    public enum FenxingType {
        TOP, // 顶分型
        BOTTOM // 底分型
    }

    /**
     * 获取时间戳
     */
    public long getTimestamp() {
        return time.toEpochMilli();
    }
}
