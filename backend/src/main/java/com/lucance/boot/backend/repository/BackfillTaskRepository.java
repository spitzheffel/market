package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.BackfillTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 回补任务仓库
 */
@Repository
public interface BackfillTaskRepository extends JpaRepository<BackfillTask, Long> {

    /**
     * 按状态查询任务
     */
    List<BackfillTask> findByStatus(BackfillTask.TaskStatus status);

    /**
     * 查询指定交易对和周期的任务
     */
    List<BackfillTask> findBySymbolAndInterval(String symbol, String interval);

    /**
     * 查询运行中的任务
     */
    default List<BackfillTask> findRunningTasks() {
        return findByStatus(BackfillTask.TaskStatus.RUNNING);
    }

    /**
     * 查询待处理的任务
     */
    default List<BackfillTask> findPendingTasks() {
        return findByStatus(BackfillTask.TaskStatus.PENDING);
    }
}
