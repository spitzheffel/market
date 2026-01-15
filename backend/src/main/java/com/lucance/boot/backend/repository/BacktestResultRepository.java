package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for BacktestResult entity CRUD operations.
 */
@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    /**
     * Find result by task ID.
     */
    Optional<BacktestResult> findByTaskId(Long taskId);

    /**
     * Check if result exists for a task.
     */
    boolean existsByTaskId(Long taskId);
}
