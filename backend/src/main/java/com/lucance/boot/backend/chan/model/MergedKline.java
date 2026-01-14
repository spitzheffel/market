package com.lucance.boot.backend.chan.model;

import com.lucance.boot.backend.entity.Kline;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理后的K线（合并包含关系后）
 * 参考: 02-phase2-chan-calculation.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergedKline {

    private int index;
    private Direction direction;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private BigDecimal close;
    private Instant time;
    private BigDecimal volume;

    @Builder.Default
    private List<Kline> elements = new ArrayList<>();

    /**
     * 方向
     */
    public enum Direction {
        UP, DOWN
    }

    /**
     * 从原始K线创建
     */
    public static MergedKline fromKline(Kline kline, int index) {
        return MergedKline.builder()
                .index(index)
                .direction(null)
                .high(kline.getHigh())
                .low(kline.getLow())
                .open(kline.getOpen())
                .close(kline.getClose())
                .time(kline.getTime())
                .volume(kline.getVolume())
                .elements(new ArrayList<>(List.of(kline)))
                .build();
    }

    /**
     * 获取时间戳
     */
    public long getTimestamp() {
        return time.toEpochMilli();
    }
}
