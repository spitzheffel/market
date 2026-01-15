package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Strategy entity CRUD operations.
 */
@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    /**
     * Find all strategies by status.
     */
    List<Strategy> findByStatus(String status);

    /**
     * Find all active strategies.
     */
    List<Strategy> findByStatusOrderByUpdatedAtDesc(String status);

    /**
     * Find strategies by name (partial match).
     */
    List<Strategy> findByNameContainingIgnoreCase(String name);

    /**
     * Find strategies that include a specific level.
     */
    @Query(value = "SELECT * FROM strategies WHERE :level = ANY(levels)", nativeQuery = true)
    List<Strategy> findByLevel(String level);

    /**
     * Find strategies with auto_trade enabled.
     */
    List<Strategy> findByAutoTradeTrue();

    /**
     * Count active strategies.
     */
    long countByStatus(String status);
}
