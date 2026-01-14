package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.service.DataIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 数据完整性控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/integrity")
@RequiredArgsConstructor
public class DataIntegrityController {

    private final DataIntegrityService dataIntegrityService;

    /**
     * 查询数据缺口
     */
    @GetMapping("/gaps")
    public ResponseEntity<List<DataIntegrityService.Gap>> findGaps(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Instant start = startTime != null
                ? Instant.ofEpochMilli(startTime)
                : Instant.now().minusSeconds(7 * 24 * 3600); // 默认7天
        Instant end = endTime != null
                ? Instant.ofEpochMilli(endTime)
                : Instant.now();

        log.info("Finding gaps: symbol={}, interval={}, start={}, end={}",
                symbol, interval, start, end);

        List<DataIntegrityService.Gap> gaps = dataIntegrityService.findGaps(
                symbol, interval, start, end);

        return ResponseEntity.ok(gaps);
    }

    /**
     * 手动触发缺口扫描
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "false") boolean autoRepair) {

        Instant start = startTime != null
                ? Instant.ofEpochMilli(startTime)
                : Instant.now().minusSeconds(7 * 24 * 3600);
        Instant end = endTime != null
                ? Instant.ofEpochMilli(endTime)
                : Instant.now();

        List<DataIntegrityService.Gap> gaps = dataIntegrityService.findGaps(
                symbol, interval, start, end);

        int repaired = 0;
        if (autoRepair && !gaps.isEmpty()) {
            repaired = dataIntegrityService.autoRepairGaps(symbol, interval, start, end);
        }

        return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "interval", interval,
                "gapsFound", gaps.size(),
                "gapsRepaired", repaired,
                "gaps", gaps));
    }

    /**
     * 获取完整性统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Instant start = startTime != null
                ? Instant.ofEpochMilli(startTime)
                : Instant.now().minusSeconds(7 * 24 * 3600);
        Instant end = endTime != null
                ? Instant.ofEpochMilli(endTime)
                : Instant.now();

        List<DataIntegrityService.Gap> gaps = dataIntegrityService.findGaps(
                symbol, interval, start, end);

        long totalMissingBars = gaps.stream()
                .mapToLong(DataIntegrityService.Gap::missingBars)
                .sum();

        return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "interval", interval,
                "startTime", start.toEpochMilli(),
                "endTime", end.toEpochMilli(),
                "gapCount", gaps.size(),
                "totalMissingBars", totalMissingBars,
                "healthStatus", gaps.isEmpty() ? "HEALTHY" : "GAPS_DETECTED"));
    }
}
