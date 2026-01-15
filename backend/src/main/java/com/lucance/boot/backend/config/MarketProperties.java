package com.lucance.boot.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场展示配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    private List<SymbolConfig> symbols = new ArrayList<>();

    @Data
    public static class SymbolConfig {
        private String symbol;
        private String type = "spot";
    }
}
