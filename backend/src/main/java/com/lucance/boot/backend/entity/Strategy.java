package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Strategy entity for trading strategy configuration.
 * Stores entry/exit conditions, position sizing, and risk control parameters.
 */
@Data
@Entity
@Table(name = "strategies")
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String version = "1.0.0";

    /**
     * Applicable time levels for this strategy, comma-separated (e.g., "1m,5m,15m")
     */
    @Column(length = 100)
    private String levels = "1m,5m,15m";

    /**
     * Entry conditions stored as JSON string.
     */
    @Column(name = "entry_conditions", columnDefinition = "TEXT")
    private String entryConditions;

    /**
     * Exit conditions stored as JSON string.
     */
    @Column(name = "exit_conditions", columnDefinition = "TEXT")
    private String exitConditions;

    /**
     * Position sizing configuration as JSON string.
     */
    @Column(name = "position_sizing", columnDefinition = "TEXT")
    private String positionSizing;

    /**
     * Risk control parameters as JSON string.
     */
    @Column(name = "risk_control", columnDefinition = "TEXT")
    private String riskControl;

    /**
     * Whether this strategy should automatically execute trades (Phase 4 feature)
     */
    @Column(name = "auto_trade")
    private Boolean autoTrade = false;

    /**
     * Status: active, inactive, archived
     */
    @Column(length = 20)
    private String status = "active";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Get levels as array.
     */
    public String[] getLevelsArray() {
        if (levels == null || levels.isBlank()) {
            return new String[0];
        }
        return levels.split(",");
    }

    /**
     * Set levels from array.
     */
    public void setLevelsArray(String[] levelsArray) {
        if (levelsArray == null || levelsArray.length == 0) {
            this.levels = "";
        } else {
            this.levels = String.join(",", levelsArray);
        }
    }
}
