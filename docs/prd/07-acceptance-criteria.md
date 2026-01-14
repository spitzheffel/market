# 验收标准和测试计划

## 总体验收标准

### 功能完整性

- [ ] 所有 Phase 1-4 的功能需求已实现
- [ ] 所有核心功能正常工作
- [ ] 所有边界情况已处理
- [ ] 所有异常情况有合理的错误提示

### 性能指标

- [ ] 数据延迟 < 100ms
- [ ] 缠论计算延迟 < 10ms（增量）
- [ ] API 响应时间 < 500ms (P95)
- [ ] 前端首屏加载 < 2s
- [ ] 支持 100 个标的并行计算

### 质量标准

- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试通过率 100%
- [ ] 代码审查通过
- [ ] 无严重和高危 bug
- [ ] 文档完整

---

## Phase 1 验收标准

### 数据源管理

**功能验收：**
- [ ] 可以成功连接 Binance、OKX、Bybit
- [ ] 可以获取历史 K线数据
- [ ] 数据格式统一且正确
- [ ] API Key 加密存储
- [ ] 延迟监控正常工作
- [ ] 延迟超过阈值时能收到告警

**性能验收：**
- [ ] 单次 API 请求延迟 < 500ms
- [ ] WebSocket 推送延迟 < 100ms
- [ ] 支持 3 个交易所同时连接

**测试用例：**
```typescript
describe('数据源管理', () => {
  test('应该能连接 Binance', async () => {
    const dataSource = new BinanceDataSource(config);
    const health = await dataSource.healthCheck();
    expect(health.status).toBe('healthy');
  });

  test('应该能获取历史K线', async () => {
    const klines = await dataSource.getKlines(
      'BTC/USDT',
      '1m',
      Date.now() - 3600000,
      Date.now()
    );
    expect(klines.length).toBeGreaterThan(0);
    expect(klines[0]).toHaveProperty('open');
    expect(klines[0]).toHaveProperty('high');
  });

  test('应该能监控延迟', async () => {
    const stats = dataSource.getLatencyStats();
    expect(stats.average).toBeLessThan(500);
  });
});
```

---

### 数据同步

**功能验收：**
- [ ] 历史数据回补正常
- [ ] 实时数据同步正常
- [ ] 缺失数据能被检测
- [ ] 缺失数据能自动修复
- [ ] 数据完整性检查正常

**性能验收：**
- [ ] 回补速度 > 1000 根/秒
- [ ] 实时数据延迟 < 100ms
- [ ] 缺失检测耗时 < 1s/天

**测试用例：**
```typescript
describe('数据同步', () => {
  test('应该能回补历史数据', async () => {
    const result = await backfill.backfill(
      'BTC/USDT',
      '1m',
      Date.now() - 86400000,
      Date.now()
    );
    expect(result.successCount).toBeGreaterThan(1400);
  });

  test('应该能检测缺失数据', async () => {
    const missing = await checker.detectMissingKlines(
      'BTC/USDT',
      '1m',
      Date.now() - 86400000,
      Date.now()
    );
    expect(Array.isArray(missing)).toBe(true);
  });

  test('应该能自动修复缺失数据', async () => {
    await autoRepair.checkAndRepair();
    // 验证缺失数据已修复
  });
});
```

---

### 基础缠论计算

**功能验收：**
- [ ] 包含关系处理正确
- [ ] 分型识别准确
- [ ] 笔的构建符合规则
- [ ] 计算结果可复现

**性能验收：**
- [ ] 全量计算 < 100ms/500根K线
- [ ] 增量计算 < 10ms

**测试用例：**
```typescript
describe('基础缠论计算', () => {
  test('应该正确处理包含关系', () => {
    const klines = loadTestData('包含关系测试数据');
    const merged = processor.processMergedKlines(klines);
    expect(merged.length).toBe(expectedLength);
  });

  test('应该正确识别分型', () => {
    const mergedKlines = loadTestData('分型测试数据');
    const fenxing = identifier.identifyFenxing(mergedKlines);
    expect(fenxing.length).toBe(expectedCount);
    expect(fenxing[0].type).toBe('top');
  });

  test('应该正确构建笔', () => {
    const fenxing = loadTestData('笔测试数据');
    const bi = builder.buildBi(fenxing);
    expect(bi.length).toBeGreaterThan(0);
    expect(bi[0].klineCount).toBeGreaterThanOrEqual(4);
  });
});
```

---

## Phase 2 验收标准

### 线段识别

**功能验收：**
- [ ] 特征序列构建正确
- [ ] 线段划分符合规则
- [ ] 线段破坏判断准确

**测试用例：**
```typescript
describe('线段识别', () => {
  test('应该正确构建特征序列', () => {
    const biList = loadTestData('线段测试数据');
    const sequence = builder.buildSequence(biList);
    expect(sequence.length).toBeGreaterThan(0);
  });

  test('应该正确判断线段破坏', () => {
    const sequence = loadTestData('线段破坏测试数据');
    const broken = builder.checkBreakdown(sequence, 'up');
    expect(broken).toBe(true);
  });
});
```

---

### 中枢识别

**功能验收：**
- [ ] 能识别至少3笔的重叠区间
- [ ] 中枢上下轨计算正确
- [ ] 中枢类型判断准确

**测试用例：**
```typescript
describe('中枢识别', () => {
  test('应该识别笔中枢', () => {
    const biList = loadTestData('中枢测试数据');
    const zhongshu = identifier.identifyBiZhongshu(biList);
    expect(zhongshu.length).toBeGreaterThan(0);
    expect(zhongshu[0].components.length).toBeGreaterThanOrEqual(3);
  });

  test('应该正确计算中枢区间', () => {
    const zhongshu = loadTestData('中枢区间测试数据');
    expect(parseFloat(zhongshu.high)).toBeGreaterThan(parseFloat(zhongshu.low));
  });
});
```

---

### 买卖点判定

**功能验收：**
- [ ] 能识别一类买卖点
- [ ] 能识别二类买卖点
- [ ] 能识别三类买卖点
- [ ] 背驰判断准确

**测试用例：**
```typescript
describe('买卖点判定', () => {
  test('应该识别一类买点', () => {
    const chanResult = loadTestData('一类买点测试数据');
    const points = identifier.identify(chanResult, macdData, klines);
    const level1Buy = points.filter(p => p.type === 'buy' && p.level === 1);
    expect(level1Buy.length).toBeGreaterThan(0);
  });

  test('应该正确判断背驰', () => {
    const xd1 = loadTestData('背驰测试数据1');
    const xd2 = loadTestData('背驰测试数据2');
    const divergence = detector.detectDivergence(xd1, xd2, macdData, klines);
    expect(divergence).not.toBeNull();
    expect(divergence.type).toBe('bullish');
  });
});
```

---

## Phase 3 验收标准

### 策略配置系统

**功能验收：**
- [ ] 可以创建和保存策略
- [ ] 可以配置入场条件
- [ ] 可以配置出场条件
- [ ] 可以配置风控参数

**测试用例：**
```typescript
describe('策略配置', () => {
  test('应该能创建策略', async () => {
    const strategy = await strategyService.createStrategy(testStrategy);
    expect(strategy.id).toBeDefined();
  });

  test('应该能更新策略', async () => {
    const updated = await strategyService.updateStrategy(strategyId, updates);
    expect(updated.name).toBe(updates.name);
  });
});
```

---

### 回测引擎

**功能验收：**
- [ ] 回测能正常运行
- [ ] 回测结果准确
- [ ] 性能统计完整
- [ ] 可视化报告正常

**性能验收：**
- [ ] 回测速度 > 10年/秒

**测试用例：**
```typescript
describe('回测引擎', () => {
  test('应该能运行回测', async () => {
    const result = await backtest.runBacktest(
      strategy,
      ['BTC/USDT'],
      startTime,
      endTime,
      10000
    );
    expect(result.totalTrades).toBeGreaterThan(0);
    expect(result.totalReturn).toBeDefined();
  });

  test('回测结果应该可复现', async () => {
    const result1 = await backtest.runBacktest(params);
    const result2 = await backtest.runBacktest(params);
    expect(result1.totalReturn).toBe(result2.totalReturn);
  });
});
```

---

### 信号推送

**功能验收：**
- [ ] 信号能及时生成
- [ ] 信号能推送到前端
- [ ] 高置信度信号能发送通知

**性能验收：**
- [ ] 信号推送延迟 < 1s

**测试用例：**
```typescript
describe('信号推送', () => {
  test('应该能生成信号', async () => {
    const signal = await signalService.evaluateStrategy(
      strategy,
      symbol,
      chanResults
    );
    expect(signal).not.toBeNull();
  });

  test('应该能推送信号', async () => {
    const received = await waitForSignal(ws, 5000);
    expect(received.type).toBe('signal');
  });
});
```

---

## Phase 4 验收标准

### 风控系统

**功能验收：**
- [ ] 风控检查正常工作
- [ ] 熔断机制有效
- [ ] 止损及时触发
- [ ] 风险监控正常

**测试用例：**
```typescript
describe('风控系统', () => {
  test('应该拒绝超过风险限制的订单', async () => {
    const result = await riskManager.canOpenPosition(
      symbol,
      largeSize,
      price,
      stopLoss,
      account
    );
    expect(result.allowed).toBe(false);
  });

  test('应该触发熔断器', async () => {
    // 模拟连续亏损
    for (let i = 0; i < 5; i++) {
      riskManager.updateState({ pnl: -1000 });
    }
    expect(riskManager.state.circuitBreakerTriggered).toBe(true);
  });
});
```

---

### 订单管理

**功能验收：**
- [ ] 订单能正确提交
- [ ] 订单状态能正确跟踪
- [ ] 订单能正确取消
- [ ] 异常情况能正确处理

**性能验收：**
- [ ] 订单提交延迟 < 100ms
- [ ] 支持 100 个并发订单

**测试用例：**
```typescript
describe('订单管理', () => {
  test('应该能提交订单', async () => {
    const order = await orderManager.submitOrder(orderRequest);
    expect(order.status).toBe('submitted');
  });

  test('应该能取消订单', async () => {
    await orderManager.cancelOrder(orderId);
    const order = await orderManager.getOrder(orderId);
    expect(order.status).toBe('cancelled');
  });

  test('应该能处理订单失败', async () => {
    // 模拟网络错误
    mockExchange.createOrder.mockRejectedValue(new Error('Network error'));
    await expect(orderManager.submitOrder(orderRequest)).rejects.toThrow();
  });
});
```

---

### 自动交易

**功能验收：**
- [ ] 信号能自动转换为订单
- [ ] 持仓能正确管理
- [ ] 止损止盈能及时触发
- [ ] 交易日志完整

**测试用例：**
```typescript
describe('自动交易', () => {
  test('应该能执行信号', async () => {
    const result = await autoTrader.executeSignal(signal, account);
    expect(result.success).toBe(true);
    expect(result.position).toBeDefined();
  });

  test('应该能触发止损', async () => {
    // 创建持仓
    const position = await positionManager.createPosition(positionRequest);

    // 模拟价格下跌
    await simulatePriceChange(symbol, stopLossPrice);

    // 等待止损触发
    await sleep(2000);

    const closedPosition = await positionManager.getPosition(position.id);
    expect(closedPosition.status).toBe('closed');
    expect(closedPosition.closeReason).toBe('stop_loss');
  });
});
```

---

## 压力测试

### 测试场景

**场景1：高并发订单提交**
```typescript
test('高并发订单提交', async () => {
  const orders = Array.from({ length: 100 }, () => createTestOrder());
  const results = await Promise.allSettled(
    orders.map(order => orderManager.submitOrder(order))
  );

  const successRate = results.filter(r => r.status === 'fulfilled').length / 100;
  expect(successRate).toBeGreaterThan(0.9); // 90%成功率
});
```

**场景2：长时间运行**
```typescript
test('7天连续运行', async () => {
  const startTime = Date.now();
  const duration = 7 * 24 * 3600 * 1000;

  while (Date.now() - startTime < duration) {
    // 模拟正常运行
    await simulateTrading();
    await sleep(60000); // 每分钟一次
  }

  // 检查内存泄漏
  const memoryUsage = process.memoryUsage();
  expect(memoryUsage.heapUsed).toBeLessThan(500 * 1024 * 1024); // < 500MB
});
```

**场景3：极端市场波动**
```typescript
test('极端市场波动', async () => {
  // 模拟价格暴跌 20%
  await simulatePriceCrash('BTC/USDT', 0.8);

  // 验证所有止损都被触发
  const openPositions = await positionManager.getOpenPositions();
  expect(openPositions.length).toBe(0);
});
```

---

## 安全测试

### SQL 注入测试
```typescript
test('应该防止 SQL 注入', async () => {
  const maliciousInput = "'; DROP TABLE users; --";
  await expect(
    strategyService.getStrategy(maliciousInput)
  ).rejects.toThrow();
});
```

### XSS 测试
```typescript
test('应该防止 XSS 攻击', async () => {
  const maliciousInput = '<script>alert("XSS")</script>';
  const strategy = await strategyService.createStrategy({
    name: maliciousInput
  });
  expect(strategy.name).not.toContain('<script>');
});
```

### API Key 安全测试
```typescript
test('API Key 应该加密存储', async () => {
  const config = await db.getExchangeConfig('binance');
  expect(config.apiKey).not.toBe(plainTextApiKey);

  const decrypted = decrypt(config.apiKey);
  expect(decrypted).toBe(plainTextApiKey);
});
```

---

## 用户验收测试 (UAT)

### 测试清单

**数据层：**
- [ ] 用户能看到实时K线数据
- [ ] 数据延迟在可接受范围内
- [ ] 历史数据完整

**计算层：**
- [ ] 缠论结构显示正确
- [ ] 买卖点标记清晰
- [ ] 计算结果符合预期

**策略层：**
- [ ] 用户能创建自定义策略
- [ ] 回测结果可信
- [ ] 信号推送及时

**交易层：**
- [ ] 订单能正确执行
- [ ] 止损止盈有效
- [ ] 风控机制可靠

---

## 性能基准测试

### 基准指标

| 指标 | 目标 | 实际 | 状态 |
|------|------|------|------|
| 数据延迟 | < 100ms | ___ | ⬜ |
| 计算延迟 | < 10ms | ___ | ⬜ |
| API 响应时间 (P95) | < 500ms | ___ | ⬜ |
| 前端首屏加载 | < 2s | ___ | ⬜ |
| 订单提交延迟 | < 100ms | ___ | ⬜ |
| 止损触发延迟 | < 1s | ___ | ⬜ |
| 并发订单数 | 100 | ___ | ⬜ |
| 并发标的数 | 100 | ___ | ⬜ |

---

## 发布检查清单

### 代码质量
- [ ] 所有测试通过
- [ ] 代码审查完成
- [ ] 无严重和高危 bug
- [ ] 代码覆盖率 > 80%

### 文档
- [ ] API 文档完整
- [ ] 用户手册完整
- [ ] 部署文档完整
- [ ] 应急预案完整

### 安全
- [ ] 安全扫描通过
- [ ] 敏感数据加密
- [ ] 权限控制正确
- [ ] 审计日志完整

### 性能
- [ ] 性能测试通过
- [ ] 压力测试通过
- [ ] 无内存泄漏
- [ ] 资源使用合理

### 运维
- [ ] 监控配置完成
- [ ] 告警规则配置
- [ ] 备份策略配置
- [ ] 回滚方案准备

---

## 总结

完成所有验收标准后，系统即可发布！

**关键成功因素：**
1. ✅ 数据质量有保障
2. ✅ 算法准确可靠
3. ✅ 性能达标
4. ✅ 风控完善
5. ✅ 测试充分

祝项目成功！🎉
