package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.BackfillTask;
import com.lucance.boot.backend.entity.BackfillTaskBatch;
import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.ExchangeAdapter;
import com.lucance.boot.backend.repository.BackfillTaskBatchRepository;
import com.lucance.boot.backend.repository.BackfillTaskRepository;
import com.lucance.boot.backend.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史数据回补服务
 * 参考: 01-phase1-data-layer.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillService {

    private final ExchangeRouterService exchangeRouterService;
    private final KlineRepository klineRepository;
    private final BackfillTaskRepository taskRepository;
    private final BackfillTaskBatchRepository batchRepository;

    // 每批次获取的K线数量
    private static final int BATCH_SIZE = 1000;

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;

    // 各周期对应的毫秒数
    private static final java.util.Map<String, Long> INTERVAL_MS = java.util.Map.of(
            "1m", 60_000L,
            "5m", 300_000L,
            "15m", 900_000L,
            "30m", 1_800_000L,
            "1h", 3_600_000L,
            "4h", 14_400_000L,
            "1d", 86_400_000L);

    /**
     * 创建回补任务
     */
    @Transactional
    public BackfillTask createTask(String symbol, String interval, long startTime, long endTime) {
        // 计算总K线数量
        long intervalMs = INTERVAL_MS.getOrDefault(interval, 60_000L);
        int totalCount = (int) ((endTime - startTime) / intervalMs);

        BackfillTask task = BackfillTask.builder()
                .symbol(symbol)
                .interval(interval)
                .startTime(startTime)
                .endTime(endTime)
                .status(BackfillTask.TaskStatus.PENDING)
                .totalCount(totalCount)
                .build();

        task = taskRepository.save(task);
        log.info("Created backfill task: id={}, symbol={}, interval={}, totalCount={}",
                task.getId(), symbol, interval, totalCount);

        return task;
    }

    /**
     * 异步执行回补任务（入口）
     * 注意：@Async 方法不能与 @Transactional 同时使用
     */
    @Async
    public void executeTaskAsync(Long taskId) {
        try {
            executeTask(taskId);
        } catch (Exception e) {
            log.error("Async task execution failed: id={}", taskId, e);
            updateTaskStatus(taskId, BackfillTask.TaskStatus.FAILED, e.getMessage());
        }
    }

    /**
     * 执行回补任务（同步，带事务）
     */
    @Transactional
    public void executeTask(Long taskId) {
        BackfillTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() != BackfillTask.TaskStatus.PENDING) {
            log.warn("Task {} is not in PENDING status, current: {}", taskId, task.getStatus());
            return;
        }

        // 更新状态为运行中
        task.setStatus(BackfillTask.TaskStatus.RUNNING);
        taskRepository.save(task);

        try {
            backfill(task);
            task.setStatus(BackfillTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            log.info("Backfill task completed: id={}, successCount={}", taskId, task.getSuccessCount());
        } catch (Exception e) {
            log.error("Backfill task failed: id={}", taskId, e);
            task.setStatus(BackfillTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }

        taskRepository.save(task);
    }

    /**
     * 更新任务状态（独立事务）
     */
    @Transactional
    public void updateTaskStatus(Long taskId, BackfillTask.TaskStatus status, String errorMessage) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }
            taskRepository.save(task);
        });
    }

    /**
     * 执行回补逻辑
     */
    private void backfill(BackfillTask task) {
        String symbol = task.getSymbol();
        String interval = task.getInterval();
        long startTime = task.getStartTime();
        long endTime = task.getEndTime();
        long intervalMs = INTERVAL_MS.getOrDefault(interval, 60_000L);

        // 计算批次
        List<long[]> batches = calculateBatches(startTime, endTime, intervalMs, BATCH_SIZE);
        int totalBatches = batches.size();
        int successCount = 0;

        log.info("Starting backfill: symbol={}, interval={}, batches={}", symbol, interval, totalBatches);

        // 创建批次记录
        List<BackfillTaskBatch> batchEntities = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            long[] batch = batches.get(i);
            BackfillTaskBatch batchEntity = BackfillTaskBatch.builder()
                    .taskId(task.getId())
                    .batchIndex(i)
                    .startTime(batch[0])
                    .endTime(batch[1])
                    .status(BackfillTaskBatch.BatchStatus.PENDING)
                    .build();
            batchEntities.add(batchEntity);
        }
        batchRepository.saveAll(batchEntities);

        // 执行每个批次
        for (int i = 0; i < batchEntities.size(); i++) {
            BackfillTaskBatch batchEntity = batchEntities.get(i);
            long batchStart = batchEntity.getStartTime();
            long batchEnd = batchEntity.getEndTime();

            try {
                // 添加限流延迟
                if (i > 0) {
                    Thread.sleep(100); // 100ms between batches
                }

                // 更新批次状态为运行中
                batchEntity.setStatus(BackfillTaskBatch.BatchStatus.RUNNING);
                batchRepository.save(batchEntity);

                // 从交易所获取数据
                ExchangeAdapter adapter = exchangeRouterService.getDefaultAdapter();
                List<Kline> klines = adapter.getKlines(symbol, interval, batchStart, batchEnd, BATCH_SIZE);

                // 保存到数据库
                if (!klines.isEmpty()) {
                    klineRepository.saveAll(klines);
                    successCount += klines.size();
                    batchEntity.setRecordCount(klines.size());
                }

                // 更新批次状态为完成
                batchEntity.setStatus(BackfillTaskBatch.BatchStatus.COMPLETED);
                batchRepository.save(batchEntity);

                // 更新任务进度
                int progress = (int) ((i + 1) * 100.0 / totalBatches);
                task.setProgress(progress);
                task.setSuccessCount(successCount);
                taskRepository.save(task);

                log.debug("Batch {}/{} completed: {} klines", i + 1, totalBatches, klines.size());

            } catch (Exception e) {
                log.warn("Batch {} failed: {}", i + 1, e.getMessage());

                // 更新批次状态为失败
                batchEntity.setStatus(BackfillTaskBatch.BatchStatus.FAILED);
                batchEntity.setErrorMessage(e.getMessage());
                batchRepository.save(batchEntity);

                // 继续处理下一批
            }
        }

        task.setSuccessCount(successCount);
    }

    /**
     * 计算批次
     */
    private List<long[]> calculateBatches(long startTime, long endTime, long intervalMs, int batchSize) {
        List<long[]> batches = new ArrayList<>();
        long batchMs = intervalMs * batchSize;

        long current = startTime;
        while (current < endTime) {
            long batchEnd = Math.min(current + batchMs, endTime);
            batches.add(new long[] { current, batchEnd });
            current = batchEnd;
        }

        return batches;
    }

    /**
     * 获取任务状态
     */
    public BackfillTask getTask(Long taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    /**
     * 获取所有任务
     */
    public List<BackfillTask> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * 取消任务
     */
    @Transactional
    public void cancelTask(Long taskId) {
        BackfillTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == BackfillTask.TaskStatus.PENDING ||
                task.getStatus() == BackfillTask.TaskStatus.RUNNING) {
            task.setStatus(BackfillTask.TaskStatus.CANCELLED);
            taskRepository.save(task);
            log.info("Cancelled backfill task: {}", taskId);
        }
    }

    /**
     * 获取任务的失败批次列表
     */
    public List<BackfillTaskBatch> getFailedBatches(Long taskId) {
        return batchRepository.findByTaskIdAndStatus(taskId, BackfillTaskBatch.BatchStatus.FAILED);
    }

    /**
     * 重试失败的批次
     */
    @Transactional
    public int retryFailedBatches(Long taskId) {
        List<BackfillTaskBatch> failedBatches = getFailedBatches(taskId);

        if (failedBatches.isEmpty()) {
            log.info("No failed batches to retry for task: {}", taskId);
            return 0;
        }

        BackfillTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        int retriedCount = 0;
        for (BackfillTaskBatch batch : failedBatches) {
            if (batch.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("Batch {} exceeded max retry count, skipping", batch.getId());
                continue;
            }

            try {
                // 增加重试计数
                batch.setRetryCount(batch.getRetryCount() + 1);
                batch.setStatus(BackfillTaskBatch.BatchStatus.RUNNING);
                batch.setErrorMessage(null);
                batchRepository.save(batch);

                // 重试获取数据
                ExchangeAdapter adapter = exchangeRouterService.getDefaultAdapter();
                List<Kline> klines = adapter.getKlines(
                        task.getSymbol(),
                        task.getInterval(),
                        batch.getStartTime(),
                        batch.getEndTime(),
                        BATCH_SIZE);

                // 保存到数据库
                if (!klines.isEmpty()) {
                    klineRepository.saveAll(klines);
                    batch.setRecordCount(klines.size());
                }

                // 更新批次状态为完成
                batch.setStatus(BackfillTaskBatch.BatchStatus.COMPLETED);
                batchRepository.save(batch);

                retriedCount++;
                log.info("Batch {} retry succeeded: {} klines", batch.getId(), klines.size());

                // 添加延迟避免限流
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Batch {} retry failed: {}", batch.getId(), e.getMessage());
                batch.setStatus(BackfillTaskBatch.BatchStatus.FAILED);
                batch.setErrorMessage(e.getMessage());
                batchRepository.save(batch);
            }
        }

        log.info("Retried {} failed batches for task {}", retriedCount, taskId);
        return retriedCount;
    }

    /**
     * 重试单个批次
     */
    @Transactional
    public boolean retryBatch(Long batchId) {
        BackfillTaskBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Batch {} exceeded max retry count", batchId);
            return false;
        }

        BackfillTask task = taskRepository.findById(batch.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + batch.getTaskId()));

        try {
            // 增加重试计数
            batch.setRetryCount(batch.getRetryCount() + 1);
            batch.setStatus(BackfillTaskBatch.BatchStatus.RUNNING);
            batch.setErrorMessage(null);
            batchRepository.save(batch);

            // 重试获取数据
            ExchangeAdapter adapter = exchangeRouterService.getDefaultAdapter();
            List<Kline> klines = adapter.getKlines(
                    task.getSymbol(),
                    task.getInterval(),
                    batch.getStartTime(),
                    batch.getEndTime(),
                    BATCH_SIZE);

            // 保存到数据库
            if (!klines.isEmpty()) {
                klineRepository.saveAll(klines);
                batch.setRecordCount(klines.size());
            }

            // 更新批次状态为完成
            batch.setStatus(BackfillTaskBatch.BatchStatus.COMPLETED);
            batchRepository.save(batch);

            log.info("Batch {} retry succeeded: {} klines", batchId, klines.size());
            return true;

        } catch (Exception e) {
            log.error("Batch {} retry failed: {}", batchId, e.getMessage());
            batch.setStatus(BackfillTaskBatch.BatchStatus.FAILED);
            batch.setErrorMessage(e.getMessage());
            batchRepository.save(batch);
            return false;
        }
    }

    /**
     * 获取任务的所有批次
     */
    public List<BackfillTaskBatch> getTaskBatches(Long taskId) {
        return batchRepository.findByTaskIdOrderByBatchIndexAsc(taskId);
    }
}
