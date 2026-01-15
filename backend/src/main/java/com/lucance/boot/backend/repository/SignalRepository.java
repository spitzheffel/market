package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.Signal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for Signal entity CRUD operations.
 */
@Repository
public interface SignalRepository extends JpaRepository<Signal, Long> {

    /**
     * Find signals by symbol and interval.
     */
    List<Signal> findBySymbolAndIntervalOrderByCreatedAtDesc(String symbol, String interval);

    /**
     * Find signals by symbol with pagination.
     */
    Page<Signal> findBySymbolOrderByCreatedAtDesc(String symbol, Pageable pageable);

    /**
     * Find recent signals within a time range.
     */
    List<Signal> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime since);

    /**
     * Find signals by status.
     */
    List<Signal> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find signals by strategy ID.
     */
    List<Signal> findByStrategyIdOrderByCreatedAtDesc(Long strategyId);

    /**
     * Find signals by type and confidence.
     */
    List<Signal> findBySignalTypeAndConfidenceOrderByCreatedAtDesc(String signalType, String confidence);

    /**
     * Find pending signals for a symbol.
     */
    @Query("SELECT s FROM Signal s WHERE s.symbol = :symbol AND s.status = 'pending' ORDER BY s.createdAt DESC")
    List<Signal> findPendingSignals(String symbol);

    /**
     * Count signals by type.
     */
    long countBySignalType(String signalType);

    /**
     * Count signals by confidence.
     */
    long countByConfidence(String confidence);
}
