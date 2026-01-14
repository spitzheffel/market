package com.lucance.boot.backend.service;

import com.lucance.boot.backend.entity.ExchangeConfig;
import com.lucance.boot.backend.repository.ExchangeConfigRepository;
import com.lucance.boot.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 交易所配置服务
 * 提供 CRUD 操作，自动加解密 API 密钥
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeConfigService {

    private final ExchangeConfigRepository repository;
    private final EncryptionUtil encryptionUtil;

    /**
     * 获取所有配置
     */
    public List<ExchangeConfig> getAllConfigs() {
        List<ExchangeConfig> configs = repository.findAll();
        configs.forEach(this::decryptSecretsForRead);
        return configs;
    }

    /**
     * 获取启用的配置
     */
    public List<ExchangeConfig> getEnabledConfigs() {
        List<ExchangeConfig> configs = repository.findByEnabledTrue();
        configs.forEach(this::decryptSecretsForRead);
        return configs;
    }

    /**
     * 获取指定交易所配置
     */
    public Optional<ExchangeConfig> getConfig(String name) {
        return repository.findById(name).map(config -> {
            decryptSecretsForRead(config);
            return config;
        });
    }

    /**
     * 保存配置（自动加密敏感信息）
     */
    @Transactional
    public ExchangeConfig saveConfig(ExchangeConfig config) {
        // 加密敏感信息
        if (config.getApiKey() != null && !encryptionUtil.isEncrypted(config.getApiKey())) {
            config.setApiKey(encryptionUtil.encrypt(config.getApiKey()));
        }
        if (config.getApiSecret() != null && !encryptionUtil.isEncrypted(config.getApiSecret())) {
            config.setApiSecret(encryptionUtil.encrypt(config.getApiSecret()));
        }
        if (config.getPassphrase() != null && !encryptionUtil.isEncrypted(config.getPassphrase())) {
            config.setPassphrase(encryptionUtil.encrypt(config.getPassphrase()));
        }

        ExchangeConfig saved = repository.save(config);
        log.info("Saved exchange config: {}", config.getName());
        return saved;
    }

    /**
     * 更新配置
     */
    @Transactional
    public ExchangeConfig updateConfig(String name, ExchangeConfig updates) {
        ExchangeConfig existing = repository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + name));

        if (updates.getEnabled() != null) {
            existing.setEnabled(updates.getEnabled());
        }
        if (updates.getApiKey() != null) {
            existing.setApiKey(encryptionUtil.encrypt(updates.getApiKey()));
        }
        if (updates.getApiSecret() != null) {
            existing.setApiSecret(encryptionUtil.encrypt(updates.getApiSecret()));
        }
        if (updates.getPassphrase() != null) {
            existing.setPassphrase(encryptionUtil.encrypt(updates.getPassphrase()));
        }
        if (updates.getConfig() != null) {
            existing.setConfig(updates.getConfig());
        }

        return repository.save(existing);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String name) {
        repository.deleteById(name);
        log.info("Deleted exchange config: {}", name);
    }

    /**
     * 解密用于读取（不修改原对象，返回新对象）
     */
    private void decryptSecretsForRead(ExchangeConfig config) {
        // 解密时创建掩码版本，不暴露真实密钥
        if (config.getApiKey() != null) {
            try {
                String decrypted = encryptionUtil.decrypt(config.getApiKey());
                config.setApiKey(maskSecret(decrypted));
            } catch (Exception e) {
                config.setApiKey("***DECRYPTION_ERROR***");
            }
        }
        if (config.getApiSecret() != null) {
            config.setApiSecret("********");
        }
        if (config.getPassphrase() != null) {
            config.setPassphrase("********");
        }
    }

    /**
     * 获取解密后的完整配置（用于内部调用）
     */
    public Optional<ExchangeConfig> getDecryptedConfig(String name) {
        return repository.findById(name).map(config -> {
            if (config.getApiKey() != null) {
                config.setApiKey(encryptionUtil.decrypt(config.getApiKey()));
            }
            if (config.getApiSecret() != null) {
                config.setApiSecret(encryptionUtil.decrypt(config.getApiSecret()));
            }
            if (config.getPassphrase() != null) {
                config.setPassphrase(encryptionUtil.decrypt(config.getPassphrase()));
            }
            return config;
        });
    }

    /**
     * 掩码处理
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
