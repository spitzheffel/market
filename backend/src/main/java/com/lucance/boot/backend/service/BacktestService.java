package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.BacktestResult;
import com.lucance.boot.backend.entity.BacktestTask;
import com.lucance.boot.backend.repository.BacktestResultRepository;
import com.lucance.boot.backend.repository.BacktestTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 回测任务管理服务
 * 负责回测任务的创建、查询、状态管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final BacktestTaskRepository backtestTaskRepository;
    private final BacktestResultRepository backtestResultRepository;

    /**
     * 创建回测任务
     */
    @Transactional
    public BacktestTask createTask(BacktestTask task) {
        validateTask(task);
        task.setStatus("pending");
        task.setProgress(0);
        log.info("Creating backtest task for strategy {}: {} symbols, {} to {}",
                task.getStrategyId(),
                task.getSymbols(),
                task.getStartTime(),
                task.getEndTime());
        return backtestTaskRepository.save(task);
    }

    /**
     * 获取回测任务
     */
    public Optional<BacktestTask> getTask(Long taskId) {
        return backtestTaskRepository.findById(taskId);
    }

    /**
     * 获取策略的所有回测任务
     */
    public List<BacktestTask> getTasksByStrategy(Long strategyId) {
        return backtestTaskRepository.findByStrategyIdOrderByCreatedAtDesc(strategyId);
    }

    /**
     * 获取指定状态的任务
     */
    public List<BacktestTask> getTasksByStatus(String status) {
        return backtestTaskRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 获取待执行的任务
     */
    public List<BacktestTask> getPendingTasks() {
        return backtestTaskRepository.findByStatusOrderByCreatedAtAsc("pending");
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public BacktestTask updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage) {
        BacktestTask task = backtestTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(status);
        if (progress != null) {
            task.setProgress(progress);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        // Update timestamps based on status
        if ("running".equals(status) && task.getStartedAt() == null) {
            task.setStartedAt(OffsetDateTime.now());
        } else if (("completed".equals(status) || "failed".equals(status) || "cancelled".equals(status))
                && task.getCompletedAt() == null) {
            task.setCompletedAt(OffsetDateTime.now());
        }

        log.info("Updated task {} status to {}, progress: {}%", taskId, status, progress);
        return backtestTaskRepository.save(task);
    }

    /**
     * 标记任务为运行中
     */
    @Transactional
    public BacktestTask markAsRunning(Long taskId) {
        return updateTaskStatus(taskId, "running", 0, null);
    }

    /**
     * 标记任务为完成
     */
    @Transactional
    public BacktestTask markAsCompleted(Long taskId) {
        return updateTaskStatus(taskId, "completed", 100, null);
    }

    /**
     * 标记任务为失败
     */
    @Transactional
    public BacktestTask markAsFailed(Long taskId, String errorMessage) {
        return updateTaskStatus(taskId, "failed", null, errorMessage);
    }

    /**
     * 取消任务
     */
    @Transactional
    public BacktestTask cancelTask(Long taskId) {
        BacktestTask task = backtestTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot cancel task in status: " + task.getStatus());
        }

        return updateTaskStatus(taskId, "cancelled", null, "Cancelled by user");
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long taskId) {
        if (!backtestTaskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        log.info("Deleting backtest task: {}", taskId);
        backtestTaskRepository.deleteById(taskId);
    }

    /**
     * 保存回测结果
     */
    @Transactional
    public BacktestResult saveResult(BacktestResult result) {
        log.info("Saving backtest result for task {}: total return {}%, max drawdown {}%",
                result.getTaskId(),
                result.getTotalReturn(),
                result.getMaxDrawdown());
        return backtestResultRepository.save(result);
    }

    /**
     * 获取回测结果
     */
    public Optional<BacktestResult> getResult(Long taskId) {
        return backtestResultRepository.findByTaskId(taskId);
    }

    /**
     * 检查任务是否有结果
     */
    public boolean hasResult(Long taskId) {
        return backtestResultRepository.existsByTaskId(taskId);
    }

    /**
     * 获取所有任务
     */
    public List<BacktestTask> getAllTasks() {
        return backtestTaskRepository.findAll();
    }

    /**
     * 统计任务数量
     */
    public long countTasksByStatus(String status) {
        return backtestTaskRepository.countByStatus(status);
    }

    /**
     * 验证任务参数
     */
    private void validateTask(BacktestTask task) {
        if (task.getStrategyId() == null) {
            throw new IllegalArgumentException("Strategy ID is required");
        }
        if (task.getSymbols() == null || task.getSymbols().isBlank()) {
            throw new IllegalArgumentException("At least one symbol is required");
        }
        if (task.getStartTime() == null || task.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (task.getStartTime().isAfter(task.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (task.getInitialCapital() == null || task.getInitialCapital().signum() <= 0) {
            throw new IllegalArgumentException("Initial capital must be positive");
        }
    }
}
