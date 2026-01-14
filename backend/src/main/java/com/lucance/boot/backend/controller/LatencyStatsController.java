package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.service.LatencyStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 延迟统计控制器
 */
@RestController
@RequestMapping("/api/stats/latency")
@RequiredArgsConstructor
public class LatencyStatsController {

    private final LatencyStatsService latencyStatsService;

    /**
     * 获取所有延迟统计
     */
    @GetMapping
    public ResponseEntity<List<LatencyStatsService.LatencyStats>> getAllStats() {
        return ResponseEntity.ok(latencyStatsService.getAllStats());
    }

    /**
     * 获取指定交易所的统计
     */
    @GetMapping("/exchange/{exchange}")
    public ResponseEntity<List<LatencyStatsService.LatencyStats>> getStatsByExchange(
            @PathVariable String exchange) {
        return ResponseEntity.ok(latencyStatsService.getStatsByExchange(exchange));
    }

    /**
     * 获取指定端点的统计
     */
    @GetMapping("/{exchange}/{endpoint}")
    public ResponseEntity<LatencyStatsService.LatencyStats> getStats(
            @PathVariable String exchange,
            @PathVariable String endpoint) {
        return latencyStatsService.getStats(exchange, endpoint)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取最近 N 秒的统计
     */
    @GetMapping("/recent")
    public ResponseEntity<List<LatencyStatsService.LatencyStats>> getRecentStats(
            @RequestParam(defaultValue = "300") int seconds) {
        return ResponseEntity.ok(latencyStatsService.getRecentStats(seconds));
    }

    /**
     * 清除统计
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearStats(
            @RequestParam(required = false) String exchange) {
        if (exchange != null) {
            latencyStatsService.clearStats(exchange);
            return ResponseEntity.ok(Map.of("cleared", exchange));
        } else {
            latencyStatsService.clearStats();
            return ResponseEntity.ok(Map.of("cleared", "all"));
        }
    }
}
