package com.lucance.boot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 回补任务批次实体
 * 记录每个批次的执行状态，支持失败重试
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "backfill_task_batches")
public class BackfillTaskBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "batch_index", nullable = false)
    private Integer batchIndex;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time", nullable = false)
    private Long endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum BatchStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (retryCount == null)
            retryCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
