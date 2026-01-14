# Phase 4: 自动交易

## 阶段概述

**时间**: 4-6 周
**目标**: 实现可以实盘运行的自动交易系统
**交付物**: 完整的自动交易系统，包含风控、订单管理和监控

---

## 阶段目标

### 核心目标
1. ✅ 实现完善的风控系统
2. ✅ 实现订单管理系统
3. ✅ 实现自动交易执行
4. ✅ 实现交易监控和告警
5. ✅ 通过压力测试

### 为什么这个阶段最关键？

**自动交易涉及真实资金：**
- 任何错误都可能导致资金损失
- 必须有完善的风控机制
- 必须有异常处理和熔断机制
- 必须经过充分测试

---

## Week 13-14: 风控系统

### 4.1 风控系统

#### 功能需求

**FR-4.1.1 资金管理**
- 单笔交易最大风险限制
- 总持仓限制
- 单个标的最大仓位
- 相关性控制

**FR-4.1.2 止损机制**
- 固定止损
- 移动止损
- 时间止损
- 最大回撤止损

**FR-4.1.3 熔断机制**
- 单日最大亏损熔断
- 连续亏损熔断
- 异常波动熔断
- 手动紧急停止

**FR-4.1.4 风险监控**
- 实时风险敞口
- 仓位分布
- 相关性分析
- 风险告警

#### 技术实现

```typescript
class RiskManager {
  private config: RiskConfig;
  private state: RiskState;

  constructor(config: RiskConfig) {
    this.config = config;
    this.state = {
      dailyPnL: 0,
      peakEquity: 0,
      currentEquity: 0,
      consecutiveLosses: 0,
      lastLossTime: 0,
      circuitBreakerTriggered: false
    };
  }

  // 检查是否可以开仓
  async canOpenPosition(
    symbol: string,
    size: number,
    price: number,
    stopLoss: number,
    account: Account
  ): Promise<RiskCheckResult> {
    // 检查1：熔断器是否触发
    if (this.state.circuitBreakerTriggered) {
      return {
        allowed: false,
        reason: '熔断器已触发，禁止开仓'
      };
    }

    // 检查2：单笔交易风险
    const positionValue = size * price;
    const riskAmount = Math.abs(price - stopLoss) * size;
    const riskRatio = riskAmount / account.totalEquity;

    if (riskRatio > this.config.maxRiskPerTrade) {
      return {
        allowed: false,
        reason: `单笔风险 ${(riskRatio * 100).toFixed(2)}% 超过限制 ${(this.config.maxRiskPerTrade * 100).toFixed(2)}%`
      };
    }

    // 检查3：单个标的最大仓位
    const existingPosition = account.positions.find(p => p.symbol === symbol);
    const totalPositionValue = existingPosition
      ? parseFloat(existingPosition.size) * parseFloat(existingPosition.currentPrice) + positionValue
      : positionValue;
    const positionRatio = totalPositionValue / account.totalEquity;

    if (positionRatio > this.config.maxPositionSize) {
      return {
        allowed: false,
        reason: `单个标的仓位 ${(positionRatio * 100).toFixed(2)}% 超过限制 ${(this.config.maxPositionSize * 100).toFixed(2)}%`
      };
    }

    // 检查4：总仓位限制
    const totalExposure = account.usedMargin + positionValue;
    const exposureRatio = totalExposure / account.totalEquity;

    if (exposureRatio > this.config.maxTotalExposure) {
      return {
        allowed: false,
        reason: `总仓位 ${(exposureRatio * 100).toFixed(2)}% 超过限制 ${(this.config.maxTotalExposure * 100).toFixed(2)}%`
      };
    }

    // 检查5：单日最大亏损
    if (this.state.dailyPnL < -this.config.maxDailyLoss * account.totalEquity) {
      return {
        allowed: false,
        reason: `单日亏损 ${this.state.dailyPnL.toFixed(2)} 达到限制`
      };
    }

    // 检查6：最大回撤
    this.state.currentEquity = account.totalEquity;
    this.state.peakEquity = Math.max(this.state.peakEquity, this.state.currentEquity);
    const drawdown = (this.state.peakEquity - this.state.currentEquity) / this.state.peakEquity;

    if (drawdown > this.config.maxDrawdown) {
      return {
        allowed: false,
        reason: `回撤 ${(drawdown * 100).toFixed(2)}% 超过限制 ${(this.config.maxDrawdown * 100).toFixed(2)}%`
      };
    }

    // 检查7：连续亏损冷静期
    if (this.state.consecutiveLosses >= this.config.maxConsecutiveLosses) {
      const cooldownRemaining = this.config.cooldownAfterLoss - (Date.now() - this.state.lastLossTime);
      if (cooldownRemaining > 0) {
        return {
          allowed: false,
          reason: `连续亏损 ${this.state.consecutiveLosses} 次，冷静期剩余 ${Math.ceil(cooldownRemaining / 60000)} 分钟`
        };
      }
    }

    // 检查8：持仓数量限制
    if (account.positions.length >= this.config.maxPositions) {
      return {
        allowed: false,
        reason: `持仓数量 ${account.positions.length} 达到限制 ${this.config.maxPositions}`
      };
    }

    // 检查9：相关性控制
    const correlation = await this.checkCorrelation(symbol, account.positions);
    if (correlation > this.config.maxCorrelation) {
      return {
        allowed: false,
        reason: `与现有持仓相关性 ${(correlation * 100).toFixed(2)}% 过高`
      };
    }

    return { allowed: true };
  }

  // 计算止损位
  calculateStopLoss(
    entryPrice: number,
    direction: 'long' | 'short',
    config: StopLossConfig,
    atr?: number
  ): number {
    switch (config.type) {
      case 'fixed':
        // 固定百分比止损
        return direction === 'long'
          ? entryPrice * (1 - config.value!)
          : entryPrice * (1 + config.value!);

      case 'atr':
        // ATR 止损
        if (!atr) throw new Error('ATR is required for ATR stop loss');
        const stopDistance = atr * config.atrMultiplier!;
        return direction === 'long'
          ? entryPrice - stopDistance
          : entryPrice + stopDistance;

      case 'fenxing':
        // 分型止损（需要从缠论结果中获取）
        // 这里简化处理
        return direction === 'long'
          ? entryPrice * (1 - 0.02)
          : entryPrice * (1 + 0.02);

      default:
        throw new Error(`Unknown stop loss type: ${config.type}`);
    }
  }

  // 更新风控状态
  updateState(trade: Trade) {
    // 更新当日盈亏
    this.state.dailyPnL += trade.pnl;

    // 更新连续亏损
    if (trade.pnl < 0) {
      this.state.consecutiveLosses++;
      this.state.lastLossTime = Date.now();
    } else {
      this.state.consecutiveLosses = 0;
    }

    // 检查是否触发熔断
    this.checkCircuitBreaker();
  }

  // 检查熔断器
  private checkCircuitBreaker() {
    // 单日亏损熔断
    if (this.state.dailyPnL < -this.config.maxDailyLoss * this.state.currentEquity) {
      this.triggerCircuitBreaker('单日亏损达到限制');
    }

    // 连续亏损熔断
    if (this.state.consecutiveLosses >= this.config.maxConsecutiveLosses) {
      this.triggerCircuitBreaker(`连续亏损 ${this.state.consecutiveLosses} 次`);
    }

    // 最大回撤熔断
    const drawdown = (this.state.peakEquity - this.state.currentEquity) / this.state.peakEquity;
    if (drawdown > this.config.maxDrawdown) {
      this.triggerCircuitBreaker(`回撤 ${(drawdown * 100).toFixed(2)}% 超过限制`);
    }
  }

  // 触发熔断器
  private triggerCircuitBreaker(reason: string) {
    this.state.circuitBreakerTriggered = true;
    console.error(`[熔断器] ${reason}`);

    // 发送告警
    this.alertService.send({
      level: 'critical',
      title: '交易熔断器触发',
      message: reason,
      timestamp: Date.now()
    });

    // 关闭所有持仓（可选）
    if (this.config.closeAllOnCircuitBreaker) {
      this.closeAllPositions();
    }
  }

  // 手动重置熔断器
  resetCircuitBreaker() {
    this.state.circuitBreakerTriggered = false;
    console.log('[熔断器] 已手动重置');
  }

  // 每日重置
  dailyReset() {
    this.state.dailyPnL = 0;
    console.log('[风控] 每日数据已重置');
  }

  // 检查相关性
  private async checkCorrelation(
    symbol: string,
    positions: Position[]
  ): Promise<number> {
    if (positions.length === 0) return 0;

    // 获取历史价格数据
    const symbolPrices = await this.getPriceHistory(symbol);
    const correlations: number[] = [];

    for (const position of positions) {
      const positionPrices = await this.getPriceHistory(position.symbol);
      const correlation = this.calculateCorrelation(symbolPrices, positionPrices);
      correlations.push(Math.abs(correlation));
    }

    return Math.max(...correlations);
  }

  private calculateCorrelation(x: number[], y: number[]): number {
    const n = Math.min(x.length, y.length);
    const meanX = x.slice(0, n).reduce((a, b) => a + b) / n;
    const meanY = y.slice(0, n).reduce((a, b) => a + b) / n;

    let numerator = 0;
    let denomX = 0;
    let denomY = 0;

    for (let i = 0; i < n; i++) {
      const dx = x[i] - meanX;
      const dy = y[i] - meanY;
      numerator += dx * dy;
      denomX += dx * dx;
      denomY += dy * dy;
    }

    return numerator / Math.sqrt(denomX * denomY);
  }
}
```

---

## Week 15-16: 订单管理 + 自动交易

### 4.2 订单管理系统

#### 功能需求

**FR-4.2.1 订单类型**
- 市价单
- 限价单
- 止损单
- 止盈单

**FR-4.2.2 订单状态管理**
- pending（待提交）
- submitted（已提交）
- partial_filled（部分成交）
- filled（完全成交）
- cancelled（已取消）
- failed（失败）

**FR-4.2.3 订单执行**
- 订单提交
- 订单取消
- 订单修改
- 订单查询

**FR-4.2.4 异常处理**
- 网络超时重试
- API 限流处理
- 余额不足处理
- 订单失败处理

#### 技术实现

```typescript
class OrderManager {
  private orders = new Map<string, Order>();
  private orderQueue: OrderQueue;

  async submitOrder(order: OrderRequest): Promise<Order> {
    // 1. 创建订单对象
    const orderObj: Order = {
      id: this.generateOrderId(),
      ...order,
      status: 'pending',
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    this.orders.set(orderObj.id, orderObj);

    try {
      // 2. 提交到交易所
      orderObj.status = 'submitted';
      const exchangeOrder = await this.exchange.createOrder(order);

      // 3. 更新订单信息
      orderObj.exchangeOrderId = exchangeOrder.id;
      orderObj.status = exchangeOrder.status;
      orderObj.updatedAt = Date.now();

      // 4. 保存到数据库
      await this.db.saveOrder(orderObj);

      // 5. 启动订单监控
      this.monitorOrder(orderObj.id);

      return orderObj;

    } catch (error) {
      // 处理错误
      orderObj.status = 'failed';
      orderObj.error = error.message;
      orderObj.updatedAt = Date.now();

      await this.db.saveOrder(orderObj);

      // 根据错误类型决定是否重试
      if (this.shouldRetry(error)) {
        return this.retryOrder(orderObj);
      }

      throw error;
    }
  }

  // 监控订单状态
  private async monitorOrder(orderId: string) {
    const order = this.orders.get(orderId);
    if (!order) return;

    const checkInterval = setInterval(async () => {
      try {
        // 查询订单状态
        const exchangeOrder = await this.exchange.getOrder(
          order.exchangeOrderId!
        );

        // 更新订单状态
        order.status = exchangeOrder.status;
        order.filledSize = exchangeOrder.filled;
        order.averagePrice = exchangeOrder.average;
        order.updatedAt = Date.now();

        await this.db.updateOrder(order);

        // 如果订单完成，停止监控
        if (order.status === 'filled' || order.status === 'cancelled') {
          clearInterval(checkInterval);

          // 触发订单完成事件
          this.emit('order-filled', order);
        }

      } catch (error) {
        console.error(`[OrderMonitor] Error monitoring order ${orderId}:`, error);
      }
    }, 1000); // 每秒检查一次

    // 设置超时
    setTimeout(() => {
      clearInterval(checkInterval);
    }, 300000); // 5分钟后停止监控
  }

  // 取消订单
  async cancelOrder(orderId: string): Promise<void> {
    const order = this.orders.get(orderId);
    if (!order) {
      throw new Error(`Order ${orderId} not found`);
    }

    try {
      await this.exchange.cancelOrder(order.exchangeOrderId!);
      order.status = 'cancelled';
      order.updatedAt = Date.now();
      await this.db.updateOrder(order);

    } catch (error) {
      console.error(`[OrderManager] Failed to cancel order ${orderId}:`, error);
      throw error;
    }
  }

  // 重试订单
  private async retryOrder(order: Order): Promise<Order> {
    const maxRetries = 3;
    let retryCount = 0;

    while (retryCount < maxRetries) {
      retryCount++;
      console.log(`[OrderManager] Retrying order ${order.id}, attempt ${retryCount}`);

      await this.sleep(1000 * retryCount); // 指数退避

      try {
        return await this.submitOrder(order);
      } catch (error) {
        if (retryCount >= maxRetries) {
          throw error;
        }
      }
    }

    throw new Error(`Failed to submit order after ${maxRetries} retries`);
  }

  private shouldRetry(error: any): boolean {
    // 网络错误、超时错误可以重试
    const retryableErrors = [
      'ETIMEDOUT',
      'ECONNRESET',
      'ENOTFOUND',
      'RequestTimeout'
    ];

    return retryableErrors.some(e => error.message.includes(e));
  }
}
```

继续下一部分...
