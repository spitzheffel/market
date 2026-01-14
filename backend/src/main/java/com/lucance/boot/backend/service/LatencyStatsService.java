package com.lucance.boot.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 延迟统计服务
 * 采集和分析 API/WebSocket 请求延迟
 */
@Slf4j
@Service
public class LatencyStatsService {

    private static final int MAX_SAMPLES = 1000;

    /**
     * 延迟样本按交易所和端点分组存储
     */
    private final Map<String, Deque<LatencySample>> samples = new ConcurrentHashMap<>();

    /**
     * 延迟样本
     */
    public record LatencySample(
            long timestamp,
            long latencyMs,
            boolean success) {
    }

    /**
     * 延迟统计结果
     */
    public record LatencyStats(
            String key,
            long sampleCount,
            double avgLatency,
            double p50Latency,
            double p95Latency,
            double p99Latency,
            long maxLatency,
            long minLatency,
            double successRate) {
    }

    /**
     * 记录请求延迟
     */
    public void recordLatency(String exchange, String endpoint, long latencyMs, boolean success) {
        String key = exchange + ":" + endpoint;
        Deque<LatencySample> deque = samples.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        deque.addLast(new LatencySample(System.currentTimeMillis(), latencyMs, success));

        // 限制样本数量
        while (deque.size() > MAX_SAMPLES) {
            deque.pollFirst();
        }

        log.debug("Recorded latency: {} = {}ms (success={})", key, latencyMs, success);
    }

    /**
     * 获取指定端点的统计
     */
    public Optional<LatencyStats> getStats(String exchange, String endpoint) {
        String key = exchange + ":" + endpoint;
        Deque<LatencySample> deque = samples.get(key);

        if (deque == null || deque.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(calculateStats(key, new ArrayList<>(deque)));
    }

    /**
     * 获取所有统计
     */
    public List<LatencyStats> getAllStats() {
        return samples.entrySet().stream()
                .map(e -> calculateStats(e.getKey(), new ArrayList<>(e.getValue())))
                .sorted(Comparator.comparing(LatencyStats::key))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定交易所的统计
     */
    public List<LatencyStats> getStatsByExchange(String exchange) {
        return samples.entrySet().stream()
                .filter(e -> e.getKey().startsWith(exchange + ":"))
                .map(e -> calculateStats(e.getKey(), new ArrayList<>(e.getValue())))
                .sorted(Comparator.comparing(LatencyStats::key))
                .collect(Collectors.toList());
    }

    /**
     * 获取最近 N 秒的统计
     */
    public List<LatencyStats> getRecentStats(int seconds) {
        long cutoff = System.currentTimeMillis() - (seconds * 1000L);

        return samples.entrySet().stream()
                .map(e -> {
                    List<LatencySample> recent = e.getValue().stream()
                            .filter(s -> s.timestamp() >= cutoff)
                            .collect(Collectors.toList());
                    return calculateStats(e.getKey(), recent);
                })
                .filter(s -> s.sampleCount() > 0)
                .sorted(Comparator.comparing(LatencyStats::key))
                .collect(Collectors.toList());
    }

    /**
     * 清除统计数据
     */
    public void clearStats() {
        samples.clear();
        log.info("Latency stats cleared");
    }

    /**
     * 清除指定交易所的统计
     */
    public void clearStats(String exchange) {
        samples.keySet().removeIf(k -> k.startsWith(exchange + ":"));
        log.info("Latency stats cleared for: {}", exchange);
    }

    /**
     * 计算统计
     */
    private LatencyStats calculateStats(String key, List<LatencySample> sampleList) {
        if (sampleList.isEmpty()) {
            return new LatencyStats(key, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        List<Long> latencies = sampleList.stream()
                .map(LatencySample::latencyMs)
                .sorted()
                .collect(Collectors.toList());

        long successCount = sampleList.stream().filter(LatencySample::success).count();
        double successRate = (double) successCount / sampleList.size() * 100;

        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long max = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
        long min = latencies.stream().mapToLong(Long::longValue).min().orElse(0);

        double p50 = percentile(latencies, 50);
        double p95 = percentile(latencies, 95);
        double p99 = percentile(latencies, 99);

        return new LatencyStats(key, sampleList.size(), avg, p50, p95, p99, max, min, successRate);
    }

    /**
     * 计算百分位数
     */
    private double percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty())
            return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
