package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.Signal;
import com.lucance.boot.backend.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing trading signals.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalService {

    private final SignalRepository signalRepository;

    /**
     * Get all signals for a symbol.
     */
    public List<Signal> getSignalsBySymbol(String symbol, String interval) {
        return signalRepository.findBySymbolAndIntervalOrderByCreatedAtDesc(symbol, interval);
    }

    /**
     * Get signals with pagination.
     */
    public Page<Signal> getSignalsBySymbol(String symbol, int page, int size) {
        return signalRepository.findBySymbolOrderByCreatedAtDesc(symbol, PageRequest.of(page, size));
    }

    /**
     * Get recent signals (last 24 hours).
     */
    public List<Signal> getRecentSignals() {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        return signalRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
    }

    /**
     * Get pending signals.
     */
    public List<Signal> getPendingSignals() {
        return signalRepository.findByStatusOrderByCreatedAtDesc("pending");
    }

    /**
     * Get signal by ID.
     */
    public Optional<Signal> getSignalById(Long id) {
        return signalRepository.findById(id);
    }

    /**
     * Create a new signal.
     */
    @Transactional
    public Signal createSignal(Signal signal) {
        validateSignal(signal);
        log.info("Creating signal: {} {} for {}", signal.getSignalType(), signal.getLevel(), signal.getSymbol());
        return signalRepository.save(signal);
    }

    /**
     * Update signal status.
     */
    @Transactional
    public Signal updateSignalStatus(Long id, String status) {
        Signal signal = signalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Signal not found: " + id));

        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        signal.setStatus(status);
        log.info("Updating signal {} status to {}", id, status);
        return signalRepository.save(signal);
    }

    /**
     * Get signals by strategy.
     */
    public List<Signal> getSignalsByStrategy(Long strategyId) {
        return signalRepository.findByStrategyIdOrderByCreatedAtDesc(strategyId);
    }

    /**
     * Get high confidence signals.
     */
    public List<Signal> getHighConfidenceSignals(String signalType) {
        return signalRepository.findBySignalTypeAndConfidenceOrderByCreatedAtDesc(signalType, "high");
    }

    /**
     * Get pending signals for a symbol.
     */
    public List<Signal> getPendingSignalsForSymbol(String symbol) {
        return signalRepository.findPendingSignals(symbol);
    }

    /**
     * Confirm a signal.
     */
    @Transactional
    public Signal confirmSignal(Long id) {
        return updateSignalStatus(id, "confirmed");
    }

    /**
     * Cancel a signal.
     */
    @Transactional
    public Signal cancelSignal(Long id) {
        return updateSignalStatus(id, "cancelled");
    }

    /**
     * Mark signal as executed.
     */
    @Transactional
    public Signal markAsExecuted(Long id) {
        return updateSignalStatus(id, "executed");
    }

    /**
     * Get signal statistics.
     */
    public SignalStats getSignalStats() {
        return new SignalStats(
                signalRepository.countBySignalType("buy"),
                signalRepository.countBySignalType("sell"),
                signalRepository.countByConfidence("high"),
                signalRepository.countByConfidence("medium"),
                signalRepository.countByConfidence("low"));
    }

    /**
     * Validate signal.
     */
    private void validateSignal(Signal signal) {
        if (signal.getSymbol() == null || signal.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (signal.getInterval() == null || signal.getInterval().isBlank()) {
            throw new IllegalArgumentException("Interval is required");
        }
        if (signal.getSignalType() == null
                || !("buy".equals(signal.getSignalType()) || "sell".equals(signal.getSignalType()))) {
            throw new IllegalArgumentException("Signal type must be 'buy' or 'sell'");
        }
        if (signal.getLevel() == null || signal.getLevel() < 1 || signal.getLevel() > 3) {
            throw new IllegalArgumentException("Level must be 1, 2, or 3");
        }
        if (signal.getConfidence() == null || !isValidConfidence(signal.getConfidence())) {
            throw new IllegalArgumentException("Confidence must be 'high', 'medium', or 'low'");
        }
    }

    private boolean isValidStatus(String status) {
        return "pending".equals(status) || "confirmed".equals(status)
                || "executed".equals(status) || "cancelled".equals(status) || "expired".equals(status);
    }

    private boolean isValidConfidence(String confidence) {
        return "high".equals(confidence) || "medium".equals(confidence) || "low".equals(confidence);
    }

    /**
     * Signal statistics record.
     */
    public record SignalStats(
            long buySignals,
            long sellSignals,
            long highConfidence,
            long mediumConfidence,
            long lowConfidence) {
    }
}
