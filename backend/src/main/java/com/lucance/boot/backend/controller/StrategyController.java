package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.Strategy;
import com.lucance.boot.backend.service.StrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for managing trading strategies.
 */
@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    /**
     * Get all strategies.
     * GET /api/strategies
     */
    @GetMapping
    public ResponseEntity<List<Strategy>> getAllStrategies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String level) {
        List<Strategy> strategies;

        if (name != null && !name.isBlank()) {
            strategies = strategyService.searchByName(name);
        } else if (level != null && !level.isBlank()) {
            strategies = strategyService.findByLevel(level);
        } else if ("active".equals(status)) {
            strategies = strategyService.getActiveStrategies();
        } else {
            strategies = strategyService.getAllStrategies();
        }

        return ResponseEntity.ok(strategies);
    }

    /**
     * Get strategy by ID.
     * GET /api/strategies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Strategy> getStrategyById(@PathVariable Long id) {
        return strategyService.getStrategyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new strategy.
     * POST /api/strategies
     */
    @PostMapping
    public ResponseEntity<Strategy> createStrategy(@RequestBody Strategy strategy) {
        try {
            Strategy created = strategyService.createStrategy(strategy);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing strategy.
     * PUT /api/strategies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Strategy> updateStrategy(
            @PathVariable Long id,
            @RequestBody Strategy strategy) {
        try {
            Strategy updated = strategyService.updateStrategy(id, strategy);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a strategy.
     * DELETE /api/strategies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategy(@PathVariable Long id) {
        try {
            strategyService.deleteStrategy(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Archive a strategy (soft delete).
     * POST /api/strategies/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<Strategy> archiveStrategy(@PathVariable Long id) {
        try {
            Strategy archived = strategyService.archiveStrategy(id);
            return ResponseEntity.ok(archived);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get strategies with auto_trade enabled.
     * GET /api/strategies/auto-trade
     */
    @GetMapping("/auto-trade")
    public ResponseEntity<List<Strategy>> getAutoTradeStrategies() {
        return ResponseEntity.ok(strategyService.getAutoTradeStrategies());
    }
}
