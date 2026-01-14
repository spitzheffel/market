package com.lucance.boot.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步和调度配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // 使用默认的异步执行器
    // 如需自定义，可以实现 AsyncConfigurer 接口
}
