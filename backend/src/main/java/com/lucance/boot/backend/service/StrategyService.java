package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.Strategy;
import com.lucance.boot.backend.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing trading strategies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;

    /**
     * Get all strategies.
     */
    public List<Strategy> getAllStrategies() {
        return strategyRepository.findAll();
    }

    /**
     * Get active strategies.
     */
    public List<Strategy> getActiveStrategies() {
        return strategyRepository.findByStatusOrderByUpdatedAtDesc("active");
    }

    /**
     * Get strategy by ID.
     */
    public Optional<Strategy> getStrategyById(Long id) {
        return strategyRepository.findById(id);
    }

    /**
     * Create a new strategy.
     */
    @Transactional
    public Strategy createStrategy(Strategy strategy) {
        validateStrategy(strategy);
        log.info("Creating strategy: {}", strategy.getName());
        return strategyRepository.save(strategy);
    }

    /**
     * Update an existing strategy.
     */
    @Transactional
    public Strategy updateStrategy(Long id, Strategy strategyUpdate) {
        Strategy existing = strategyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + id));

        // Update fields
        if (strategyUpdate.getName() != null) {
            existing.setName(strategyUpdate.getName());
        }
        if (strategyUpdate.getDescription() != null) {
            existing.setDescription(strategyUpdate.getDescription());
        }
        if (strategyUpdate.getLevels() != null) {
            existing.setLevels(strategyUpdate.getLevels());
        }
        if (strategyUpdate.getEntryConditions() != null) {
            existing.setEntryConditions(strategyUpdate.getEntryConditions());
        }
        if (strategyUpdate.getExitConditions() != null) {
            existing.setExitConditions(strategyUpdate.getExitConditions());
        }
        if (strategyUpdate.getPositionSizing() != null) {
            existing.setPositionSizing(strategyUpdate.getPositionSizing());
        }
        if (strategyUpdate.getRiskControl() != null) {
            existing.setRiskControl(strategyUpdate.getRiskControl());
        }
        if (strategyUpdate.getAutoTrade() != null) {
            existing.setAutoTrade(strategyUpdate.getAutoTrade());
        }
        if (strategyUpdate.getStatus() != null) {
            existing.setStatus(strategyUpdate.getStatus());
        }

        // Increment version
        String currentVersion = existing.getVersion();
        existing.setVersion(incrementVersion(currentVersion));

        log.info("Updating strategy: {} to version {}", existing.getName(), existing.getVersion());
        return strategyRepository.save(existing);
    }

    /**
     * Delete a strategy.
     */
    @Transactional
    public void deleteStrategy(Long id) {
        if (!strategyRepository.existsById(id)) {
            throw new IllegalArgumentException("Strategy not found: " + id);
        }
        log.info("Deleting strategy: {}", id);
        strategyRepository.deleteById(id);
    }

    /**
     * Archive a strategy (soft delete).
     */
    @Transactional
    public Strategy archiveStrategy(Long id) {
        Strategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + id));
        strategy.setStatus("archived");
        log.info("Archiving strategy: {}", strategy.getName());
        return strategyRepository.save(strategy);
    }

    /**
     * Find strategies by name.
     */
    public List<Strategy> searchByName(String name) {
        return strategyRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Find strategies by level.
     */
    public List<Strategy> findByLevel(String level) {
        return strategyRepository.findByLevel(level);
    }

    /**
     * Get strategies with auto_trade enabled.
     */
    public List<Strategy> getAutoTradeStrategies() {
        return strategyRepository.findByAutoTradeTrue();
    }

    /**
     * Validate strategy configuration.
     */
    private void validateStrategy(Strategy strategy) {
        if (strategy.getName() == null || strategy.getName().isBlank()) {
            throw new IllegalArgumentException("Strategy name is required");
        }
        if (strategy.getLevels() == null || strategy.getLevels().isBlank()) {
            throw new IllegalArgumentException("At least one level is required");
        }
        if (strategy.getEntryConditions() == null || strategy.getEntryConditions().isBlank()) {
            throw new IllegalArgumentException("Entry conditions are required");
        }
        if (strategy.getExitConditions() == null || strategy.getExitConditions().isBlank()) {
            throw new IllegalArgumentException("Exit conditions are required");
        }

        // Validate levels
        for (String level : strategy.getLevelsArray()) {
            if (!isValidLevel(level)) {
                throw new IllegalArgumentException("Invalid level: " + level);
            }
        }
    }

    /**
     * Check if a level is valid.
     */
    private boolean isValidLevel(String level) {
        return level.matches("\\d+[mhdwM]"); // e.g., 1m, 5m, 15m, 1h, 1d, 1w, 1M
    }

    /**
     * Increment version string (e.g., "1.0.0" -> "1.0.1").
     */
    private String incrementVersion(String version) {
        if (version == null || version.isBlank()) {
            return "1.0.0";
        }
        String[] parts = version.split("\\.");
        if (parts.length == 3) {
            int patch = Integer.parseInt(parts[2]) + 1;
            return parts[0] + "." + parts[1] + "." + patch;
        }
        return "1.0.0";
    }
}
