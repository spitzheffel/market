package com.lucance.boot.backend.repository;

import com.lucance.boot.backend.entity.ExchangeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 交易所配置仓库
 */
@Repository
public interface ExchangeConfigRepository extends JpaRepository<ExchangeConfig, String> {

    List<ExchangeConfig> findByEnabledTrue();
}
