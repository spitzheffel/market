package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * K线数据实体
 * 存储标准化的K线数据，支持多交易所
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "klines")
@IdClass(KlineId.class)
public class Kline {

    @Id
    @Column(name = "symbol", length = 20, nullable = false)
    private String symbol;

    @Id
    @Column(name = "interval", length = 10, nullable = false)
    private String interval;

    @Id
    @Column(name = "time", nullable = false)
    private Instant time;

    @Column(name = "open", precision = 20, scale = 8, nullable = false)
    private BigDecimal open;

    @Column(name = "high", precision = 20, scale = 8, nullable = false)
    private BigDecimal high;

    @Column(name = "low", precision = 20, scale = 8, nullable = false)
    private BigDecimal low;

    @Column(name = "close", precision = 20, scale = 8, nullable = false)
    private BigDecimal close;

    @Column(name = "volume", precision = 20, scale = 8, nullable = false)
    private BigDecimal volume;

    /**
     * 从时间戳创建 Kline
     */
    public static Kline fromTimestamp(String symbol, String interval, long timestamp,
                                       BigDecimal open, BigDecimal high, BigDecimal low,
                                       BigDecimal close, BigDecimal volume) {
        return Kline.builder()
                .symbol(symbol)
                .interval(interval)
                .time(Instant.ofEpochMilli(timestamp))
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }

    /**
     * 获取时间戳（毫秒）
     */
    public long getTimestamp() {
        return time.toEpochMilli();
    }
}
