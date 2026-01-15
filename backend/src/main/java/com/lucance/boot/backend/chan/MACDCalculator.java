package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.MACDResult;
import com.lucance.boot.backend.entity.Kline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * MACD计算器
 * 参考: 02-phase2-chan-calculation.md
 * 
 * MACD = EMA(12) - EMA(26)
 * Signal = EMA(MACD, 9)
 * Histogram = (MACD - Signal) * 2
 */
@Slf4j
@Component
public class MACDCalculator {

    private static final int FAST_PERIOD = 12;
    private static final int SLOW_PERIOD = 26;
    private static final int SIGNAL_PERIOD = 9;
    private static final int SCALE = 8;

    /**
     * 计算MACD
     */
    public List<MACDResult> calculate(List<Kline> klines) {
        List<MACDResult> results = new ArrayList<>();

        if (klines.size() < SLOW_PERIOD) {
            log.debug("K线数量不足 {}, 无法计算MACD", klines.size());
            return results;
        }

        // 提取收盘价
        List<BigDecimal> closes = klines.stream()
                .map(Kline::getClose)
                .toList();

        // 计算EMA12和EMA26
        List<BigDecimal> ema12 = calculateEMA(closes, FAST_PERIOD);
        List<BigDecimal> ema26 = calculateEMA(closes, SLOW_PERIOD);

        // 计算DIF (MACD Line)
        List<BigDecimal> dif = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            if (i < SLOW_PERIOD - 1) {
                dif.add(null);
            } else {
                BigDecimal difValue = ema12.get(i).subtract(ema26.get(i));
                dif.add(difValue);
            }
        }

        // 计算DEA (Signal Line)
        List<BigDecimal> dea = calculateEMAWithNulls(dif, SIGNAL_PERIOD, SLOW_PERIOD - 1);

        // 构建结果
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal difVal = dif.get(i);
            BigDecimal deaVal = dea.get(i);
            BigDecimal macdVal = null;

            if (difVal != null && deaVal != null) {
                // MACD柱 = (DIF - DEA) * 2
                macdVal = difVal.subtract(deaVal).multiply(BigDecimal.valueOf(2));
            }

            results.add(MACDResult.builder()
                    .timestamp(klines.get(i).getTime().toEpochMilli())
                    .dif(difVal)
                    .dea(deaVal)
                    .macd(macdVal)
                    .build());
        }

        log.debug("计算MACD完成: {} 条记录", results.size());
        return results;
    }

    /**
     * 计算EMA (指数移动平均)
     */
    private List<BigDecimal> calculateEMA(List<BigDecimal> data, int period) {
        List<BigDecimal> ema = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        // 第一个EMA值使用SMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period && i < data.size(); i++) {
            sum = sum.add(data.get(i));
            ema.add(null); // 前period-1个值为null
        }

        if (data.size() >= period) {
            BigDecimal firstEma = sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
            ema.set(period - 1, firstEma);

            // 后续值使用EMA公式: EMA = price * k + EMA_prev * (1-k)
            for (int i = period; i < data.size(); i++) {
                BigDecimal prevEma = ema.get(i - 1);
                BigDecimal currentEma = data.get(i).multiply(multiplier)
                        .add(prevEma.multiply(oneMinusMultiplier))
                        .setScale(SCALE, RoundingMode.HALF_UP);
                ema.add(currentEma);
            }
        }

        return ema;
    }

    /**
     * 计算带null值的EMA (用于DIF序列)
     */
    private List<BigDecimal> calculateEMAWithNulls(List<BigDecimal> data, int period, int startIndex) {
        List<BigDecimal> ema = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        // 前startIndex + period - 1个值为null
        int firstValidIndex = startIndex + period - 1;
        for (int i = 0; i < data.size(); i++) {
            if (i < firstValidIndex) {
                ema.add(null);
            } else if (i == firstValidIndex) {
                // 计算第一个EMA值 (SMA)
                BigDecimal sum = BigDecimal.ZERO;
                int count = 0;
                for (int j = startIndex; j <= i; j++) {
                    if (data.get(j) != null) {
                        sum = sum.add(data.get(j));
                        count++;
                    }
                }
                if (count > 0) {
                    ema.add(sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP));
                } else {
                    ema.add(null);
                }
            } else {
                BigDecimal prevEma = ema.get(i - 1);
                BigDecimal currentVal = data.get(i);
                if (prevEma != null && currentVal != null) {
                    BigDecimal currentEma = currentVal.multiply(multiplier)
                            .add(prevEma.multiply(oneMinusMultiplier))
                            .setScale(SCALE, RoundingMode.HALF_UP);
                    ema.add(currentEma);
                } else {
                    ema.add(prevEma);
                }
            }
        }

        return ema;
    }

    /**
     * 计算指定时间范围内的MACD面积
     */
    public BigDecimal calculateMACDArea(List<MACDResult> macdData, long startTime, long endTime) {
        return macdData.stream()
                .filter(m -> m.getTimestamp() >= startTime && m.getTimestamp() <= endTime)
                .filter(m -> m.getMacd() != null)
                .map(m -> m.getMacd().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
