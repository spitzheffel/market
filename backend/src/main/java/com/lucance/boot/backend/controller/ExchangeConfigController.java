package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.ExchangeConfig;
import com.lucance.boot.backend.service.ExchangeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交易所配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/exchange-configs")
@RequiredArgsConstructor
public class ExchangeConfigController {

    private final ExchangeConfigService exchangeConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public ResponseEntity<List<ExchangeConfig>> getAllConfigs() {
        return ResponseEntity.ok(exchangeConfigService.getAllConfigs());
    }

    /**
     * 获取启用的配置
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<ExchangeConfig>> getEnabledConfigs() {
        return ResponseEntity.ok(exchangeConfigService.getEnabledConfigs());
    }

    /**
     * 获取指定配置
     */
    @GetMapping("/{name}")
    public ResponseEntity<ExchangeConfig> getConfig(@PathVariable String name) {
        return exchangeConfigService.getConfig(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建配置
     */
    @PostMapping
    public ResponseEntity<ExchangeConfig> createConfig(@RequestBody ExchangeConfig config) {
        log.info("Creating exchange config: {}", config.getName());
        ExchangeConfig saved = exchangeConfigService.saveConfig(config);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新配置
     */
    @PutMapping("/{name}")
    public ResponseEntity<ExchangeConfig> updateConfig(
            @PathVariable String name,
            @RequestBody ExchangeConfig config) {
        log.info("Updating exchange config: {}", name);
        ExchangeConfig updated = exchangeConfigService.updateConfig(name, config);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable String name) {
        log.info("Deleting exchange config: {}", name);
        exchangeConfigService.deleteConfig(name);
        return ResponseEntity.ok(Map.of("deleted", name));
    }
}
