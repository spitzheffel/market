package com.lucance.boot.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 交易所配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "exchange")
public class ExchangeProperties {

    private ProxySettings proxy = new ProxySettings();
    private BinanceConfig binance = new BinanceConfig();
    private OkxConfig okx = new OkxConfig();
    private BybitConfig bybit = new BybitConfig();

    @Data
    public static class ProxySettings {
        private boolean enabled = false;
        private String type = "HTTP"; // HTTP, SOCKS
        private String host = "127.0.0.1";
        private Integer port = 7890;
        private String username;
        private String password;
    }

    @Data
    public static class BinanceConfig {
        private boolean enabled = true;
        private String apiKey;
        private String secretKey;
        private String baseUrl = "https://api.binance.com";
        private String wsUrl = "wss://stream.binance.com:9443/ws";
        private RateLimitConfig rateLimit = new RateLimitConfig();
    }

    @Data
    public static class OkxConfig {
        private boolean enabled = false;
        private String apiKey;
        private String secretKey;
        private String passphrase;
        private String baseUrl = "https://www.okx.com";
    }

    @Data
    public static class BybitConfig {
        private boolean enabled = false;
        private String apiKey;
        private String secretKey;
        private String baseUrl = "https://api.bybit.com";
    }

    @Data
    public static class RateLimitConfig {
        private int requestsPerSecond = 10;
        private int requestsPerMinute = 1200;
    }
}
