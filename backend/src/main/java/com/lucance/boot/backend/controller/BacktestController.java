package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.BacktestResult;
import com.lucance.boot.backend.entity.BacktestTask;
import com.lucance.boot.backend.service.BacktestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 回测任务管理 REST API
 */
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    /**
     * 创建回测任务
     * POST /api/backtest/tasks
     */
    @PostMapping("/tasks")
    public ResponseEntity<BacktestTask> createTask(@RequestBody BacktestTask task) {
        try {
            BacktestTask created = backtestService.createTask(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取回测任务
     * GET /api/backtest/tasks/{id}
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<BacktestTask> getTask(@PathVariable Long id) {
        return backtestService.getTask(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有回测任务
     * GET /api/backtest/tasks?strategyId=1&status=completed
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<BacktestTask>> getTasks(
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) String status) {
        List<BacktestTask> tasks;

        if (strategyId != null) {
            tasks = backtestService.getTasksByStrategy(strategyId);
        } else if (status != null) {
            tasks = backtestService.getTasksByStatus(status);
        } else {
            tasks = backtestService.getAllTasks();
        }

        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取待执行的任务
     * GET /api/backtest/tasks/pending
     */
    @GetMapping("/tasks/pending")
    public ResponseEntity<List<BacktestTask>> getPendingTasks() {
        return ResponseEntity.ok(backtestService.getPendingTasks());
    }

    /**
     * 启动回测任务
     * POST /api/backtest/tasks/{id}/start
     *
     * Note: This endpoint marks the task as ready to run.
     * The actual execution is handled by BacktestEngine (can be triggered by a scheduler or manually).
     */
    @PostMapping("/tasks/{id}/start")
    public ResponseEntity<BacktestTask> startTask(@PathVariable Long id) {
        try {
            BacktestTask task = backtestService.getTask(id)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

            if (!"pending".equals(task.getStatus())) {
                return ResponseEntity.badRequest().build();
            }

            // Mark as running - actual execution will be handled by BacktestEngine
            BacktestTask updated = backtestService.markAsRunning(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 取消回测任务
     * POST /api/backtest/tasks/{id}/cancel
     */
    @PostMapping("/tasks/{id}/cancel")
    public ResponseEntity<BacktestTask> cancelTask(@PathVariable Long id) {
        try {
            BacktestTask cancelled = backtestService.cancelTask(id);
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除回测任务
     * DELETE /api/backtest/tasks/{id}
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        try {
            backtestService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取回测结果
     * GET /api/backtest/results/{taskId}
     */
    @GetMapping("/results/{taskId}")
    public ResponseEntity<BacktestResult> getResult(@PathVariable Long taskId) {
        return backtestService.getResult(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取任务统计
     * GET /api/backtest/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = Map.of(
                "pending", backtestService.countTasksByStatus("pending"),
                "running", backtestService.countTasksByStatus("running"),
                "completed", backtestService.countTasksByStatus("completed"),
                "failed", backtestService.countTasksByStatus("failed"),
                "cancelled", backtestService.countTasksByStatus("cancelled")
        );
        return ResponseEntity.ok(stats);
    }
}
