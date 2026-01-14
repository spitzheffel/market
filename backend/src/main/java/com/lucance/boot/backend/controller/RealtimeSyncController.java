package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.service.RealtimeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 实时同步控制器
 * 支持多交易所订阅
 */
@Slf4j
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeSyncController {

    private final RealtimeSyncService realtimeSyncService;

    /**
     * 订阅 K线数据
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "true") boolean saveToDb,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Subscribe request: exchange={}, symbol={}, interval={}, saveToDb={}",
                exchange, symbol, interval, saveToDb);

        realtimeSyncService.subscribe(exchange, symbol, interval, saveToDb, null);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "exchange", exchange,
                "symbol", symbol,
                "interval", interval));
    }

    /**
     * 取消订阅
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "binance") String exchange) {

        log.info("Unsubscribe request: exchange={}, symbol={}, interval={}", exchange, symbol, interval);

        realtimeSyncService.unsubscribe(exchange, symbol, interval);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "exchange", exchange,
                "symbol", symbol,
                "interval", interval));
    }

    /**
     * 获取活动订阅列表
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<Set<String>> getSubscriptions() {
        return ResponseEntity.ok(realtimeSyncService.getActiveSubscriptions());
    }

    /**
     * 获取可用交易所
     */
    @GetMapping("/exchanges")
    public ResponseEntity<Set<String>> getExchanges() {
        return ResponseEntity.ok(realtimeSyncService.getAvailableExchanges());
    }

    /**
     * 获取连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "subscriptions", realtimeSyncService.getActiveSubscriptions(),
                "count", realtimeSyncService.getActiveSubscriptions().size(),
                "availableExchanges", realtimeSyncService.getAvailableExchanges()));
    }
}
