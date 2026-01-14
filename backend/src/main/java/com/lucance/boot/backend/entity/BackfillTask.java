package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 回补任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backfill_tasks")
public class BackfillTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", length = 20, nullable = false)
    private String symbol;

    @Column(name = "interval", length = 10, nullable = false)
    private String interval;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time", nullable = false)
    private Long endTime;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "total_count")
    @Builder.Default
    private Integer totalCount = 0;

    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
