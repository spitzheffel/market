package com.lucance.boot.backend.exchange;

/**
 * 交易所 API 异常
 */
public class ExchangeApiException extends RuntimeException {

    private final int code;
    private final String exchangeName;

    public ExchangeApiException(String message) {
        super(message);
        this.code = -1;
        this.exchangeName = null;
    }

    public ExchangeApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.exchangeName = null;
    }

    public ExchangeApiException(String exchangeName, int code, String message) {
        super(String.format("[%s] Error %d: %s", exchangeName, code, message));
        this.code = code;
        this.exchangeName = exchangeName;
    }

    public int getCode() {
        return code;
    }

    public String getExchangeName() {
        return exchangeName;
    }
}
