package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.entity.BackfillTask;
import com.lucance.boot.backend.entity.BackfillTaskBatch;
import com.lucance.boot.backend.service.BackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 回补任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/backfill")
@RequiredArgsConstructor
public class BackfillController {

    private final BackfillService backfillService;

    /**
     * 创建回补任务
     */
    @PostMapping
    public ResponseEntity<BackfillTask> createTask(@RequestBody CreateTaskRequest request) {
        log.info("Creating backfill task: {}", request);

        BackfillTask task = backfillService.createTask(
                request.symbol(),
                request.interval(),
                request.startTime(),
                request.endTime());

        // 异步执行任务
        if (Boolean.TRUE.equals(request.autoExecute())) {
            backfillService.executeTaskAsync(task.getId());
        }

        return ResponseEntity.ok(task);
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<BackfillTask> getTask(@PathVariable Long taskId) {
        BackfillTask task = backfillService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    /**
     * 获取所有任务
     */
    @GetMapping
    public ResponseEntity<List<BackfillTask>> getAllTasks() {
        return ResponseEntity.ok(backfillService.getAllTasks());
    }

    @PostMapping("/{taskId}/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@PathVariable Long taskId) {
        BackfillTask task = backfillService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        backfillService.executeTaskAsync(taskId);
        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "status", "queued"));
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> cancelTask(@PathVariable Long taskId) {
        backfillService.cancelTask(taskId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取任务的所有批次
     */
    @GetMapping("/{taskId}/batches")
    public ResponseEntity<List<BackfillTaskBatch>> getTaskBatches(@PathVariable Long taskId) {
        return ResponseEntity.ok(backfillService.getTaskBatches(taskId));
    }

    /**
     * 获取任务的失败批次
     */
    @GetMapping("/{taskId}/batches/failed")
    public ResponseEntity<List<BackfillTaskBatch>> getFailedBatches(@PathVariable Long taskId) {
        return ResponseEntity.ok(backfillService.getFailedBatches(taskId));
    }

    /**
     * 重试任务的所有失败批次
     */
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryFailedBatches(@PathVariable Long taskId) {
        int retriedCount = backfillService.retryFailedBatches(taskId);
        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "retriedCount", retriedCount,
                "message", "Retried " + retriedCount + " failed batches"));
    }

    /**
     * 重试单个批次
     */
    @PostMapping("/batches/{batchId}/retry")
    public ResponseEntity<Map<String, Object>> retryBatch(@PathVariable Long batchId) {
        boolean success = backfillService.retryBatch(batchId);
        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "success", success,
                "message", success ? "Batch retry succeeded" : "Batch retry failed"));
    }

    public record CreateTaskRequest(
            String symbol,
            String interval,
            long startTime,
            long endTime,
            Boolean autoExecute) {
    }
}
