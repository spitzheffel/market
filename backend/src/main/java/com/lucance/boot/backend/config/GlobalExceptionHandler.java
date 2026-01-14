package com.lucance.boot.backend.config;

import com.lucance.boot.backend.exchange.ExchangeApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理交易所 API 异常
     */
    @ExceptionHandler(ExchangeApiException.class)
    public ResponseEntity<Map<String, Object>> handleExchangeApiException(ExchangeApiException e) {
        log.error("Exchange API error", e);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "EXCHANGE_API_ERROR",
                "message", e.getMessage(),
                "code", e.getCode(),
                "timestamp", Instant.now().toEpochMilli()));
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_ARGUMENT",
                "message", e.getMessage(),
                "timestamp", Instant.now().toEpochMilli()));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unexpected error", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred",
                "timestamp", Instant.now().toEpochMilli()));
    }
}
