package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.Signal;
import com.lucance.boot.backend.service.SignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for managing trading signals.
 */
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalService signalService;

    /**
     * Get signals for a symbol.
     * GET /api/signals?symbol=BTCUSDT&interval=1m
     */
    @GetMapping
    public ResponseEntity<List<Signal>> getSignals(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) Long strategyId) {
        List<Signal> signals;

        if (strategyId != null) {
            signals = signalService.getSignalsByStrategy(strategyId);
        } else if (symbol != null && interval != null) {
            signals = signalService.getSignalsBySymbol(symbol, interval);
        } else {
            signals = signalService.getRecentSignals();
        }

        return ResponseEntity.ok(signals);
    }

    /**
     * Get signals with pagination.
     * GET /api/signals/paged?symbol=BTCUSDT&page=0&size=20
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<Signal>> getSignalsPaged(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(signalService.getSignalsBySymbol(symbol, page, size));
    }

    /**
     * Get pending signals.
     * GET /api/signals/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Signal>> getPendingSignals(
            @RequestParam(required = false) String symbol) {
        if (symbol != null) {
            return ResponseEntity.ok(signalService.getPendingSignalsForSymbol(symbol));
        }
        return ResponseEntity.ok(signalService.getPendingSignals());
    }

    /**
     * Get high confidence signals.
     * GET /api/signals/high-confidence?type=buy
     */
    @GetMapping("/high-confidence")
    public ResponseEntity<List<Signal>> getHighConfidenceSignals(
            @RequestParam(defaultValue = "buy") String type) {
        return ResponseEntity.ok(signalService.getHighConfidenceSignals(type));
    }

    /**
     * Get signal by ID.
     * GET /api/signals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Signal> getSignalById(@PathVariable Long id) {
        return signalService.getSignalById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new signal.
     * POST /api/signals
     */
    @PostMapping
    public ResponseEntity<Signal> createSignal(@RequestBody Signal signal) {
        try {
            Signal created = signalService.createSignal(signal);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Confirm a signal.
     * POST /api/signals/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Signal> confirmSignal(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(signalService.confirmSignal(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancel a signal.
     * POST /api/signals/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Signal> cancelSignal(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(signalService.cancelSignal(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark signal as executed.
     * POST /api/signals/{id}/execute
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Signal> executeSignal(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(signalService.markAsExecuted(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get signal statistics.
     * GET /api/signals/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<SignalService.SignalStats> getSignalStats() {
        return ResponseEntity.ok(signalService.getSignalStats());
    }
}
