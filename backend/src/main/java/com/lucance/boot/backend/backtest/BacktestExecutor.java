package com.lucance.boot.backend.backtest;

import com.lucance.boot.backend.entity.BacktestTask;
import com.lucance.boot.backend.service.BacktestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回测任务执行器
 * 定期检查并执行待处理的回测任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestExecutor {

    private final BacktestService backtestService;
    private final BacktestEngine backtestEngine;

    // Thread pool for executing backtests
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * 定期检查并执行待处理的回测任务
     * 每30秒检查一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void executeBacktests() {
        try {
            // Get running tasks
            List<BacktestTask> runningTasks = backtestService.getTasksByStatus("running");

            if (runningTasks.isEmpty()) {
                return;
            }

            log.debug("Found {} running backtest tasks", runningTasks.size());

            // Execute each running task
            for (BacktestTask task : runningTasks) {
                executorService.submit(() -> {
                    try {
                        log.info("Executing backtest task {}", task.getId());
                        backtestEngine.executeBacktest(task.getId());
                    } catch (Exception e) {
                        log.error("Error executing backtest task {}: {}", task.getId(), e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error in backtest executor: {}", e.getMessage(), e);
        }
    }

    /**
     * Shutdown executor on application stop
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
