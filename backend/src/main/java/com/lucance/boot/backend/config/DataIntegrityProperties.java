package com.lucance.boot.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "data-integrity")
public class DataIntegrityProperties {
    private boolean autoExecuteBackfill = false;
}
