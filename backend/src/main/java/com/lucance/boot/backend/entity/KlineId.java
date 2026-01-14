package com.lucance.boot.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Kline 复合主键
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KlineId implements Serializable {
    private String symbol;
    private String interval;
    private Instant time;
}
