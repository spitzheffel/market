package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.BacktestTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for BacktestTask entity CRUD operations.
 */
@Repository
public interface BacktestTaskRepository extends JpaRepository<BacktestTask, Long> {

    /**
     * Find tasks by strategy ID.
     */
    List<BacktestTask> findByStrategyIdOrderByCreatedAtDesc(Long strategyId);

    /**
     * Find tasks by status.
     */
    List<BacktestTask> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find pending tasks.
     */
    List<BacktestTask> findByStatusOrderByCreatedAtAsc(String status);

    /**
     * Count tasks by status.
     */
    long countByStatus(String status);
}
