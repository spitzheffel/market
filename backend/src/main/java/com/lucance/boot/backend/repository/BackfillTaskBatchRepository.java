package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.BackfillTaskBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 回补任务批次仓库
 */
@Repository
public interface BackfillTaskBatchRepository extends JpaRepository<BackfillTaskBatch, Long> {

    List<BackfillTaskBatch> findByTaskId(Long taskId);

    List<BackfillTaskBatch> findByTaskIdAndStatus(Long taskId, BackfillTaskBatch.BatchStatus status);

    List<BackfillTaskBatch> findByStatus(BackfillTaskBatch.BatchStatus status);

    long countByTaskIdAndStatus(Long taskId, BackfillTaskBatch.BatchStatus status);

    List<BackfillTaskBatch> findByTaskIdOrderByBatchIndexAsc(Long taskId);
}
