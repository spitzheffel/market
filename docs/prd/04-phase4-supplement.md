# Phase 4 补充：自动交易执行 + 监控

## Week 17-18: 自动交易执行 + 监控告警

### 4.3 自动交易执行

#### 功能需求

**FR-4.3.1 信号转订单**
- 信号验证和过滤
- 仓位计算
- 订单参数生成
- 订单提交

**FR-4.3.2 持仓管理**
- 持仓跟踪
- 止损止盈监控
- 移动止损更新
- 持仓平仓

**FR-4.3.3 异常处理**
- 订单失败重试
- 网络异常处理
- 交易所异常处理
- 数据异常处理

#### 技术实现

```typescript
class AutoTrader {
  private riskManager: RiskManager;
  private orderManager: OrderManager;
  private positionManager: PositionManager;

  async executeSignal(
    signal: Signal,
    account: Account
  ): Promise<ExecutionResult> {
    try {
      // 1. 风控检查
      const riskCheck = await this.riskManager.canOpenPosition(
        signal.symbol,
        signal.positionSize,
        parseFloat(signal.entryPrice),
        parseFloat(signal.stopLoss),
        account
      );

      if (!riskCheck.allowed) {
        console.log(`[AutoTrader] Signal rejected: ${riskCheck.reason}`);
        await this.updateSignalStatus(signal.id, 'ignored', riskCheck.reason);
        return {
          success: false,
          reason: riskCheck.reason
        };
      }

      // 2. 计算实际仓位
      const positionSize = this.calculatePositionSize(
        signal,
        account,
        parseFloat(signal.stopLoss)
      );

      // 3. 创建订单
      const order = await this.orderManager.submitOrder({
        symbol: signal.symbol,
        type: 'market',
        side: signal.type === 'buy' ? 'buy' : 'sell',
        size: positionSize,
        stopLoss: signal.stopLoss,
        takeProfit: signal.takeProfit
      });

      // 4. 等待订单成交
      await this.waitForOrderFilled(order.id);

      // 5. 创建持仓记录
      const position = await this.positionManager.createPosition({
        orderId: order.id,
        symbol: signal.symbol,
        side: signal.type === 'buy' ? 'long' : 'short',
        entryPrice: order.averagePrice!,
        entryTime: Date.now(),
        size: order.filledSize!,
        stopLoss: signal.stopLoss,
        takeProfit: signal.takeProfit,
        strategyId: signal.strategyId,
        signalId: signal.id
      });

      // 6. 启动持仓监控
      this.monitorPosition(position.id);

      // 7. 更新信号状态
      await this.updateSignalStatus(signal.id, 'executed');

      return {
        success: true,
        order,
        position
      };

    } catch (error) {
      console.error(`[AutoTrader] Failed to execute signal:`, error);
      await this.updateSignalStatus(signal.id, 'failed', error.message);
      return {
        success: false,
        reason: error.message
      };
    }
  }

  private calculatePositionSize(
    signal: Signal,
    account: Account,
    stopLoss: number
  ): number {
    const entryPrice = parseFloat(signal.entryPrice);
    const riskAmount = account.totalEquity * 0.02; // 单笔风险 2%
    const priceRisk = Math.abs(entryPrice - stopLoss);
    const positionSize = riskAmount / priceRisk;

    // 限制最大仓位
    const maxPositionValue = account.totalEquity * 0.1; // 单个标的最大 10%
    const maxSize = maxPositionValue / entryPrice;

    return Math.min(positionSize, maxSize);
  }

  private async waitForOrderFilled(
    orderId: string,
    timeout: number = 30000
  ): Promise<void> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
      const order = await this.orderManager.getOrder(orderId);

      if (order.status === 'filled') {
        return;
      }

      if (order.status === 'cancelled' || order.status === 'failed') {
        throw new Error(`Order ${orderId} ${order.status}`);
      }

      await this.sleep(1000);
    }

    throw new Error(`Order ${orderId} timeout`);
  }
}
```

---

### 4.4 持仓管理

#### 功能需求

**FR-4.4.1 持仓跟踪**
- 实时价格更新
- 未实现盈亏计算
- 持仓状态管理

**FR-4.4.2 止损止盈**
- 固定止损
- 移动止损
- 固定止盈
- 时间止损

**FR-4.4.3 持仓平仓**
- 手动平仓
- 自动平仓
- 部分平仓
- 全部平仓

#### 技术实现

```typescript
class PositionManager {
  private positions = new Map<string, Position>();
  private monitors = new Map<string, NodeJS.Timeout>();

  async createPosition(request: PositionRequest): Promise<Position> {
    const position: Position = {
      id: this.generatePositionId(),
      ...request,
      currentPrice: request.entryPrice,
      unrealizedPnL: 0,
      realizedPnL: 0,
      status: 'open',
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    this.positions.set(position.id, position);
    await this.db.savePosition(position);

    return position;
  }

  monitorPosition(positionId: string) {
    const position = this.positions.get(positionId);
    if (!position) return;

    // 每秒检查一次
    const monitor = setInterval(async () => {
      try {
        // 1. 更新当前价格
        const currentPrice = await this.getCurrentPrice(position.symbol);
        position.currentPrice = currentPrice;

        // 2. 计算未实现盈亏
        position.unrealizedPnL = this.calculateUnrealizedPnL(position);

        // 3. 检查止损
        if (this.shouldStopLoss(position)) {
          await this.closePosition(position.id, 'stop_loss');
          return;
        }

        // 4. 检查止盈
        if (this.shouldTakeProfit(position)) {
          await this.closePosition(position.id, 'take_profit');
          return;
        }

        // 5. 更新移动止损
        if (position.trailingStop) {
          this.updateTrailingStop(position);
        }

        // 6. 检查时间止损
        if (this.shouldTimeStop(position)) {
          await this.closePosition(position.id, 'time_stop');
          return;
        }

        // 7. 保存更新
        await this.db.updatePosition(position);

      } catch (error) {
        console.error(`[PositionMonitor] Error monitoring position ${positionId}:`, error);
      }
    }, 1000);

    this.monitors.set(positionId, monitor);
  }

  private shouldStopLoss(position: Position): boolean {
    const currentPrice = parseFloat(position.currentPrice);
    const stopLoss = parseFloat(position.stopLoss);

    if (position.side === 'long') {
      return currentPrice <= stopLoss;
    } else {
      return currentPrice >= stopLoss;
    }
  }

  private shouldTakeProfit(position: Position): boolean {
    if (!position.takeProfit) return false;

    const currentPrice = parseFloat(position.currentPrice);
    const takeProfit = parseFloat(position.takeProfit);

    if (position.side === 'long') {
      return currentPrice >= takeProfit;
    } else {
      return currentPrice <= takeProfit;
    }
  }

  private updateTrailingStop(position: Position) {
    if (!position.trailingStop) return;

    const currentPrice = parseFloat(position.currentPrice);
    const entryPrice = parseFloat(position.entryPrice);
    const currentStopLoss = parseFloat(position.stopLoss);

    if (position.side === 'long') {
      // 多头：价格上涨时，止损跟随上移
      const trailingDistance = currentPrice * position.trailingStop.distance;
      const newStopLoss = currentPrice - trailingDistance;

      if (newStopLoss > currentStopLoss) {
        position.stopLoss = newStopLoss.toString();
        console.log(`[PositionManager] Updated trailing stop for ${position.id}: ${newStopLoss}`);
      }
    } else {
      // 空头：价格下跌时，止损跟随下移
      const trailingDistance = currentPrice * position.trailingStop.distance;
      const newStopLoss = currentPrice + trailingDistance;

      if (newStopLoss < currentStopLoss) {
        position.stopLoss = newStopLoss.toString();
        console.log(`[PositionManager] Updated trailing stop for ${position.id}: ${newStopLoss}`);
      }
    }
  }

  private shouldTimeStop(position: Position): boolean {
    // 如果持仓时间超过设定时间，平仓
    const holdingTime = Date.now() - position.entryTime;
    const maxHoldingTime = 24 * 3600 * 1000; // 24小时

    return holdingTime > maxHoldingTime;
  }

  async closePosition(
    positionId: string,
    reason: string
  ): Promise<void> {
    const position = this.positions.get(positionId);
    if (!position) {
      throw new Error(`Position ${positionId} not found`);
    }

    try {
      // 1. 提交平仓订单
      const closeOrder = await this.orderManager.submitOrder({
        symbol: position.symbol,
        type: 'market',
        side: position.side === 'long' ? 'sell' : 'buy',
        size: position.size
      });

      // 2. 等待订单成交
      await this.waitForOrderFilled(closeOrder.id);

      // 3. 更新持仓状态
      position.status = 'closed';
      position.closeReason = reason;
      position.exitPrice = closeOrder.averagePrice!;
      position.exitTime = Date.now();
      position.realizedPnL = this.calculateRealizedPnL(position);
      position.updatedAt = Date.now();

      await this.db.updatePosition(position);

      // 4. 停止监控
      const monitor = this.monitors.get(positionId);
      if (monitor) {
        clearInterval(monitor);
        this.monitors.delete(positionId);
      }

      // 5. 更新风控状态
      this.riskManager.updateState({
        pnl: position.realizedPnL,
        timestamp: Date.now()
      });

      console.log(`[PositionManager] Closed position ${positionId}: ${reason}, PnL: ${position.realizedPnL}`);

    } catch (error) {
      console.error(`[PositionManager] Failed to close position ${positionId}:`, error);
      throw error;
    }
  }

  private calculateUnrealizedPnL(position: Position): number {
    const entryPrice = parseFloat(position.entryPrice);
    const currentPrice = parseFloat(position.currentPrice);
    const size = position.size;

    if (position.side === 'long') {
      return (currentPrice - entryPrice) * size;
    } else {
      return (entryPrice - currentPrice) * size;
    }
  }

  private calculateRealizedPnL(position: Position): number {
    const entryPrice = parseFloat(position.entryPrice);
    const exitPrice = parseFloat(position.exitPrice!);
    const size = position.size;

    if (position.side === 'long') {
      return (exitPrice - entryPrice) * size;
    } else {
      return (entryPrice - exitPrice) * size;
    }
  }
}
```

---

### 4.5 监控和告警

#### 功能需求

**FR-4.5.1 系统监控**
- 服务健康检查
- 性能指标监控
- 资源使用监控
- 错误率监控

**FR-4.5.2 交易监控**
- 订单状态监控
- 持仓状态监控
- 盈亏监控
- 风险指标监控

**FR-4.5.3 告警通知**
- 多级别告警（info/warning/error/critical）
- 多渠道通知（邮件/短信/推送/钉钉）
- 告警聚合和去重
- 告警确认和处理

#### 技术实现

```typescript
class MonitoringService {
  private metrics = new Map<string, Metric>();
  private alerts = new Map<string, Alert>();

  // 系统健康检查
  async healthCheck(): Promise<HealthReport> {
    const checks = await Promise.all([
      this.checkDatabase(),
      this.checkRedis(),
      this.checkKafka(),
      this.checkExchanges(),
      this.checkServices()
    ]);

    const allHealthy = checks.every(c => c.status === 'healthy');

    return {
      status: allHealthy ? 'healthy' : 'unhealthy',
      checks,
      timestamp: Date.now()
    };
  }

  // 性能指标收集
  collectMetrics() {
    setInterval(async () => {
      // 系统指标
      this.metrics.set('cpu_usage', await this.getCpuUsage());
      this.metrics.set('memory_usage', await this.getMemoryUsage());
      this.metrics.set('disk_usage', await this.getDiskUsage());

      // 应用指标
      this.metrics.set('request_qps', await this.getRequestQps());
      this.metrics.set('response_time', await this.getResponseTime());
      this.metrics.set('error_rate', await this.getErrorRate());

      // 业务指标
      this.metrics.set('active_positions', await this.getActivePositions());
      this.metrics.set('daily_pnl', await this.getDailyPnL());
      this.metrics.set('total_trades', await this.getTotalTrades());

      // 检查告警规则
      await this.checkAlertRules();

    }, 60000); // 每分钟收集一次
  }

  // 检查告警规则
  private async checkAlertRules() {
    const rules = await this.db.getAlertRules();

    for (const rule of rules) {
      const metric = this.metrics.get(rule.metric);
      if (!metric) continue;

      const triggered = this.evaluateRule(rule, metric.value);

      if (triggered) {
        await this.triggerAlert(rule, metric.value);
      }
    }
  }

  private evaluateRule(rule: AlertRule, value: number): boolean {
    switch (rule.operator) {
      case '>':
        return value > rule.threshold;
      case '<':
        return value < rule.threshold;
      case '>=':
        return value >= rule.threshold;
      case '<=':
        return value <= rule.threshold;
      case '==':
        return value === rule.threshold;
      default:
        return false;
    }
  }

  private async triggerAlert(rule: AlertRule, value: number) {
    // 检查是否已经触发过（去重）
    const alertKey = `${rule.id}_${Math.floor(Date.now() / 300000)}`; // 5分钟内去重
    if (this.alerts.has(alertKey)) {
      return;
    }

    const alert: Alert = {
      id: this.generateAlertId(),
      ruleId: rule.id,
      level: rule.level,
      title: rule.title,
      message: `${rule.metric} ${rule.operator} ${rule.threshold}, current: ${value}`,
      value,
      timestamp: Date.now(),
      status: 'pending'
    };

    this.alerts.set(alertKey, alert);
    await this.db.saveAlert(alert);

    // 发送通知
    await this.sendNotification(alert);
  }

  private async sendNotification(alert: Alert) {
    const channels = this.getNotificationChannels(alert.level);

    for (const channel of channels) {
      try {
        switch (channel) {
          case 'email':
            await this.emailService.send({
              to: this.config.alertEmail,
              subject: `[${alert.level.toUpperCase()}] ${alert.title}`,
              body: alert.message
            });
            break;

          case 'sms':
            if (alert.level === 'critical') {
              await this.smsService.send({
                to: this.config.alertPhone,
                message: `${alert.title}: ${alert.message}`
              });
            }
            break;

          case 'dingtalk':
            await this.dingtalkService.send({
              webhook: this.config.dingtalkWebhook,
              title: alert.title,
              text: alert.message,
              level: alert.level
            });
            break;

          case 'push':
            await this.pushService.send({
              userId: this.config.userId,
              title: alert.title,
              body: alert.message,
              data: alert
            });
            break;
        }
      } catch (error) {
        console.error(`[Monitoring] Failed to send notification via ${channel}:`, error);
      }
    }
  }

  private getNotificationChannels(level: string): string[] {
    switch (level) {
      case 'critical':
        return ['email', 'sms', 'dingtalk', 'push'];
      case 'error':
        return ['email', 'dingtalk', 'push'];
      case 'warning':
        return ['dingtalk', 'push'];
      case 'info':
        return ['push'];
      default:
        return [];
    }
  }
}
```

---

### 4.6 压力测试

#### 测试场景

**场景1：高并发订单提交**
```typescript
async function testConcurrentOrders() {
  const orderCount = 100;
  const orders = Array.from({ length: orderCount }, (_, i) => ({
    symbol: 'BTC/USDT',
    type: 'market',
    side: i % 2 === 0 ? 'buy' : 'sell',
    size: 0.001
  }));

  const startTime = Date.now();
  const results = await Promise.allSettled(
    orders.map(order => orderManager.submitOrder(order))
  );
  const endTime = Date.now();

  const successCount = results.filter(r => r.status === 'fulfilled').length;
  const failureCount = results.filter(r => r.status === 'rejected').length;
  const avgTime = (endTime - startTime) / orderCount;

  console.log(`
    Total Orders: ${orderCount}
    Success: ${successCount}
    Failure: ${failureCount}
    Success Rate: ${(successCount / orderCount * 100).toFixed(2)}%
    Avg Time: ${avgTime.toFixed(2)}ms
  `);

  // 验收标准：成功率 > 90%，平均时间 < 200ms
  expect(successCount / orderCount).toBeGreaterThan(0.9);
  expect(avgTime).toBeLessThan(200);
}
```

**场景2：长时间运行稳定性**
```typescript
async function testLongRunning() {
  const duration = 7 * 24 * 3600 * 1000; // 7天
  const startTime = Date.now();
  const startMemory = process.memoryUsage().heapUsed;

  let tradeCount = 0;
  let errorCount = 0;

  while (Date.now() - startTime < duration) {
    try {
      // 模拟正常交易流程
      await simulateTrading();
      tradeCount++;
    } catch (error) {
      errorCount++;
      console.error('Trading error:', error);
    }

    // 每分钟检查一次
    await sleep(60000);

    // 每小时输出统计
    if (tradeCount % 60 === 0) {
      const currentMemory = process.memoryUsage().heapUsed;
      const memoryGrowth = currentMemory - startMemory;
      console.log(`
        Running Time: ${((Date.now() - startTime) / 3600000).toFixed(2)}h
        Trade Count: ${tradeCount}
        Error Count: ${errorCount}
        Error Rate: ${(errorCount / tradeCount * 100).toFixed(2)}%
        Memory Growth: ${(memoryGrowth / 1024 / 1024).toFixed(2)}MB
      `);
    }
  }

  // 验收标准：错误率 < 1%，内存增长 < 500MB
  expect(errorCount / tradeCount).toBeLessThan(0.01);
  expect(process.memoryUsage().heapUsed - startMemory).toBeLessThan(500 * 1024 * 1024);
}
```

**场景3：极端市场波动**
```typescript
async function testExtremeVolatility() {
  // 模拟价格暴跌 20%
  const initialPrice = 50000;
  const crashPrice = initialPrice * 0.8;

  // 创建测试持仓
  const positions = await createTestPositions(10, initialPrice);

  // 模拟价格暴跌
  await simulatePriceCrash('BTC/USDT', crashPrice);

  // 等待止损触发
  await sleep(5000);

  // 验证所有持仓都已平仓
  const openPositions = await positionManager.getOpenPositions();
  expect(openPositions.length).toBe(0);

  // 验证熔断器已触发
  expect(riskManager.state.circuitBreakerTriggered).toBe(true);

  // 验证所有止损都在合理范围内
  for (const position of positions) {
    const closedPosition = await positionManager.getPosition(position.id);
    expect(closedPosition.status).toBe('closed');
    expect(closedPosition.closeReason).toBe('stop_loss');

    const loss = parseFloat(closedPosition.realizedPnL);
    const maxLoss = position.size * initialPrice * 0.05; // 最大亏损 5%
    expect(Math.abs(loss)).toBeLessThan(maxLoss);
  }
}
```

---

## Phase 4 交付物

### 代码交付物
- [x] 风控系统
- [x] 订单管理系统
- [x] 自动交易执行器
- [x] 持仓管理系统
- [x] 监控告警系统

### 测试交付物
- [ ] 风控系统测试
- [ ] 订单管理测试
- [ ] 自动交易测试
- [ ] 压力测试报告
- [ ] 安全测试报告

### 文档交付物
- [ ] 风控配置指南
- [ ] 自动交易使用手册
- [ ] 监控告警配置
- [ ] 应急预案
- [ ] 运维手册

---

## 验收标准

### 功能验收
- [ ] 风控系统正常工作
- [ ] 订单能正确提交和执行
- [ ] 持仓能正确管理
- [ ] 止损止盈及时触发
- [ ] 监控告警正常

### 性能验收
- [ ] 订单提交延迟 < 100ms
- [ ] 止损触发延迟 < 1s
- [ ] 支持 100 个并发订单
- [ ] 7天连续运行无内存泄漏

### 安全验收
- [ ] API Key 加密存储
- [ ] 所有操作有审计日志
- [ ] 风控机制有效
- [ ] 熔断器正常工作

---

## 风险和应对

### 技术风险

**风险1：交易所 API 限流**
- 影响：订单提交失败
- 应对：实现请求队列和限流控制

**风险2：网络延迟**
- 影响：止损不及时
- 应对：多重止损机制（本地 + 交易所）

**风险3：数据异常**
- 影响：错误的交易决策
- 应对：多重数据验证和异常检测

### 业务风险

**风险1：策略失效**
- 影响：持续亏损
- 应对：实时监控策略性能，自动停止失效策略

**风险2：黑天鹅事件**
- 影响：巨额亏损
- 应对：严格的风控限制和熔断机制

**风险3：系统故障**
- 影响：无法及时止损
- 应对：多重监控和告警，应急预案

---

## 下一步

完成 Phase 4 后，系统即可投入实盘运行！

**上线前检查清单：**
1. ✅ 所有功能测试通过
2. ✅ 压力测试通过
3. ✅ 安全测试通过
4. ✅ 监控告警配置完成
5. ✅ 应急预案准备完成
6. ✅ 团队培训完成

**持续优化方向：**
- 策略优化和调参
- 性能优化
- 新功能开发
- 用户体验改进

祝项目成功！🚀
