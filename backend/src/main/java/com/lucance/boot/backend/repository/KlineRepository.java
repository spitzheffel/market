package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.entity.KlineId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * K线数据仓库
 */
@Repository
public interface KlineRepository extends JpaRepository<Kline, KlineId> {

        /**
         * 查询指定范围的K线数据
         */
        @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.interval = :interval " +
                        "AND k.time >= :startTime AND k.time < :endTime ORDER BY k.time ASC")
        List<Kline> findBySymbolAndIntervalAndTimeRange(
                        @Param("symbol") String symbol,
                        @Param("interval") String interval,
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime);

        /**
         * 查询最新的N条K线（使用 Pageable）
         */
        @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.interval = :interval " +
                        "ORDER BY k.time DESC")
        List<Kline> findLatestKlines(
                        @Param("symbol") String symbol,
                        @Param("interval") String interval,
                        Pageable pageable);

        /**
         * 查询最新的N条K线（便捷方法）
         */
        default List<Kline> findLatestKlines(String symbol, String interval, int limit) {
                return findLatestKlines(symbol, interval, Pageable.ofSize(limit));
        }

        /**
         * 获取最新一条K线（使用 Pageable）
         */
        @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.interval = :interval " +
                        "ORDER BY k.time DESC")
        List<Kline> findTopBySymbolAndInterval(
                        @Param("symbol") String symbol,
                        @Param("interval") String interval,
                        Pageable pageable);

        /**
         * 获取最新一条K线（便捷方法）
         */
        default Kline findLatestKline(String symbol, String interval) {
                List<Kline> result = findTopBySymbolAndInterval(symbol, interval, Pageable.ofSize(1));
                return result.isEmpty() ? null : result.get(0);
        }

        /**
         * 统计K线数量
         */
        long countBySymbolAndInterval(String symbol, String interval);

        /**
         * 检查是否存在指定时间的K线
         */
        boolean existsBySymbolAndIntervalAndTime(String symbol, String interval, Instant time);
}
