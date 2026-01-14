package com.lucance.boot.backend.service;

import com.lucance.boot.backend.config.DataIntegrityProperties;
import com.lucance.boot.backend.entity.BackfillTask;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据完整性服务
 * 检测 K 线数据缺口并触发自动回补
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataIntegrityService {

    private final KlineRepository klineRepository;
    private final BackfillService backfillService;
    private final DataIntegrityProperties dataIntegrityProperties;

    /**
     * 数据缺口记录
     */
    public record Gap(
            String symbol,
            String interval,
            Instant start,
            Instant end,
            long missingBars) {
    }

    /**
     * 检测指定交易对和周期的数据缺口
     */
    public List<Gap> findGaps(String symbol, String interval, Instant startTime, Instant endTime) {
        List<Gap> gaps = new ArrayList<>();

        // 获取时间范围内的所有 K 线
        List<Kline> klines = klineRepository.findBySymbolAndIntervalAndTimeRange(
                symbol, interval, startTime, endTime);

        if (klines.isEmpty()) {
            // 整个范围都缺失
            long expectedBars = calculateExpectedBars(interval, startTime, endTime);
            gaps.add(new Gap(symbol, interval, startTime, endTime, expectedBars));
            return gaps;
        }

        // 计算每个周期的时长
        Duration intervalDuration = parseIntervalDuration(interval);
        if (intervalDuration == null) {
            log.warn("Unknown interval: {}", interval);
            return gaps;
        }

        // 检查开头是否有缺口
        Kline first = klines.get(0);
        if (first.getTime().isAfter(startTime)) {
            long missingBars = Duration.between(startTime, first.getTime()).toMillis()
                    / intervalDuration.toMillis();
            if (missingBars > 0) {
                gaps.add(new Gap(symbol, interval, startTime, first.getTime(), missingBars));
            }
        }

        // 检查中间的缺口
        for (int i = 0; i < klines.size() - 1; i++) {
            Kline current = klines.get(i);
            Kline next = klines.get(i + 1);

            Instant expectedNext = current.getTime().plus(intervalDuration);
            if (next.getTime().isAfter(expectedNext)) {
                long missingBars = Duration.between(expectedNext, next.getTime()).toMillis()
                        / intervalDuration.toMillis();
                if (missingBars > 0) {
                    gaps.add(new Gap(symbol, interval, expectedNext, next.getTime(), missingBars));
                }
            }
        }

        // 检查末尾是否有缺口
        Kline last = klines.get(klines.size() - 1);
        Instant expectedEnd = last.getTime().plus(intervalDuration);
        if (expectedEnd.isBefore(endTime)) {
            long missingBars = Duration.between(expectedEnd, endTime).toMillis()
                    / intervalDuration.toMillis();
            if (missingBars > 0) {
                gaps.add(new Gap(symbol, interval, expectedEnd, endTime, missingBars));
            }
        }

        log.info("Found {} gaps for {} {} between {} and {}",
                gaps.size(), symbol, interval, startTime, endTime);
        return gaps;
    }

    /**
     * 自动修复缺口
     */
    public int autoRepairGaps(String symbol, String interval, Instant startTime, Instant endTime) {
        List<Gap> gaps = findGaps(symbol, interval, startTime, endTime);

        int repairedCount = 0;
        for (Gap gap : gaps) {
            try {
                BackfillTask task = backfillService.createTask(
                        symbol, interval,
                        gap.start().toEpochMilli(),
                        gap.end().toEpochMilli());
                if (dataIntegrityProperties.isAutoExecuteBackfill()) {
                    backfillService.executeTaskAsync(task.getId());
                    log.info("Auto-executing backfill task {}", task.getId());
                }
                repairedCount++;
                log.info("Created backfill task for gap: {} {} {} - {}",
                        symbol, interval, gap.start(), gap.end());
            } catch (Exception e) {
                log.error("Failed to create backfill task for gap", e);
            }
        }

        return repairedCount;
    }

    /**
     * 定时扫描检测缺口（每小时执行一次）
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledGapScan() {
        log.info("Starting scheduled gap scan...");

        // 可配置的扫描目标
        List<Map<String, String>> targets = List.of(
                Map.of("symbol", "BTC/USDT", "interval", "1h"),
                Map.of("symbol", "BTC/USDT", "interval", "1d"),
                Map.of("symbol", "ETH/USDT", "interval", "1h"));

        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(Duration.ofDays(7)); // 扫描最近7天

        for (Map<String, String> target : targets) {
            String symbol = target.get("symbol");
            String interval = target.get("interval");

            try {
                List<Gap> gaps = findGaps(symbol, interval, startTime, endTime);
                if (!gaps.isEmpty()) {
                    log.warn("Found {} gaps for {} {}", gaps.size(), symbol, interval);
                    autoRepairGaps(symbol, interval, startTime, endTime);
                }
            } catch (Exception e) {
                log.error("Gap scan failed for {} {}", symbol, interval, e);
            }
        }

        log.info("Scheduled gap scan completed");
    }

    /**
     * 解析周期时长
     */
    private Duration parseIntervalDuration(String interval) {
        return switch (interval) {
            case "1m" -> Duration.ofMinutes(1);
            case "3m" -> Duration.ofMinutes(3);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "30m" -> Duration.ofMinutes(30);
            case "1h" -> Duration.ofHours(1);
            case "2h" -> Duration.ofHours(2);
            case "4h" -> Duration.ofHours(4);
            case "6h" -> Duration.ofHours(6);
            case "12h" -> Duration.ofHours(12);
            case "1d" -> Duration.ofDays(1);
            case "1w" -> Duration.ofDays(7);
            default -> null;
        };
    }

    /**
     * 计算预期的 K 线数量
     */
    private long calculateExpectedBars(String interval, Instant start, Instant end) {
        Duration intervalDuration = parseIntervalDuration(interval);
        if (intervalDuration == null)
            return 0;
        return Duration.between(start, end).toMillis() / intervalDuration.toMillis();
    }
}
