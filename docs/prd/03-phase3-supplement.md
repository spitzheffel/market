# Phase 3 补充：多级别联立 + 信号推送

## Week 11-12: 多级别联立 + 信号推送

### 3.3 多级别联立分析

#### 功能需求

**FR-3.3.1 级别关系映射**
- 小级别结构在大级别中的位置
- 级别间的时间对齐
- 级别转换规则

**FR-3.3.2 多级别信号综合**
- 不同级别信号的权重
- 信号冲突的处理
- 综合置信度计算

**FR-3.3.3 级别共振检测**
- 多个级别同时出现信号
- 共振强度评估
- 共振类型分类

#### 技术实现

```typescript
class MultiLevelAnalyzer {
  async analyze(
    symbol: string,
    levels: string[]
  ): Promise<MultiLevelResult> {
    // 1. 获取各级别的缠论计算结果
    const chanResults = new Map<string, ChanResult>();
    for (const level of levels) {
      const result = await this.chanEngine.getResult(symbol, level);
      chanResults.set(level, result);
    }

    // 2. 建立级别间的关系
    const relationships = this.buildLevelRelationships(chanResults);

    // 3. 检测多级别共振
    const resonances = this.detectResonances(chanResults, relationships);

    // 4. 综合评估
    const evaluation = this.evaluateMultiLevel(
      chanResults,
      relationships,
      resonances
    );

    return {
      chanResults,
      relationships,
      resonances,
      evaluation
    };
  }

  private buildLevelRelationships(
    chanResults: Map<string, ChanResult>
  ): LevelRelationships {
    const relationships: LevelRelationships = {};

    // 建立相邻级别的映射关系
    const levels = Array.from(chanResults.keys()).sort(
      (a, b) => this.getLevelMinutes(a) - this.getLevelMinutes(b)
    );

    for (let i = 0; i < levels.length - 1; i++) {
      const smallLevel = levels[i];
      const largeLevel = levels[i + 1];

      relationships[`${smallLevel}_in_${largeLevel}`] =
        this.mapSmallToLarge(
          chanResults.get(smallLevel)!,
          chanResults.get(largeLevel)!
        );
    }

    return relationships;
  }

  private mapSmallToLarge(
    smallResult: ChanResult,
    largeResult: ChanResult
  ): LevelMapping {
    // 判断小级别的当前位置在大级别的哪个结构中
    const currentTime = Date.now();

    // 找到大级别当前的笔
    const currentLargeBi = largeResult.bi.find(
      bi => bi.startTime <= currentTime && bi.endTime >= currentTime
    );

    // 找到大级别当前的线段
    const currentLargeXianduan = largeResult.xianduan.find(
      xd => xd.startTime <= currentTime && xd.endTime >= currentTime
    );

    // 找到大级别当前的中枢
    const currentLargeZhongshu = largeResult.zhongshu.find(
      zs => zs.startTime <= currentTime && zs.endTime >= currentTime
    );

    return {
      currentBi: currentLargeBi,
      currentXianduan: currentLargeXianduan,
      currentZhongshu: currentLargeZhongshu,
      position: this.calculatePosition(smallResult, largeResult)
    };
  }

  private detectResonances(
    chanResults: Map<string, ChanResult>,
    relationships: LevelRelationships
  ): Resonance[] {
    const resonances: Resonance[] = [];

    // 检测买点共振
    const buyResonance = this.detectBuyResonance(chanResults);
    if (buyResonance) {
      resonances.push(buyResonance);
    }

    // 检测卖点共振
    const sellResonance = this.detectSellResonance(chanResults);
    if (sellResonance) {
      resonances.push(sellResonance);
    }

    // 检测趋势共振
    const trendResonance = this.detectTrendResonance(chanResults);
    if (trendResonance) {
      resonances.push(trendResonance);
    }

    return resonances;
  }

  private detectBuyResonance(
    chanResults: Map<string, ChanResult>
  ): Resonance | null {
    const levels = Array.from(chanResults.keys());
    const buySignals: { level: string; point: TradingPoint }[] = [];

    // 检查每个级别是否有买点
    for (const level of levels) {
      const result = chanResults.get(level)!;
      const recentBuyPoint = result.tradingPoints
        .filter(p => p.type === 'buy')
        .sort((a, b) => b.timestamp - a.timestamp)[0];

      if (recentBuyPoint &&
          Date.now() - recentBuyPoint.timestamp < 3600000) {
        buySignals.push({ level, point: recentBuyPoint });
      }
    }

    // 如果多个级别都有买点，形成共振
    if (buySignals.length >= 2) {
      return {
        type: 'buy',
        levels: buySignals.map(s => s.level),
        signals: buySignals,
        strength: this.calculateResonanceStrength(buySignals),
        timestamp: Date.now()
      };
    }

    return null;
  }

  private calculateResonanceStrength(
    signals: Array<{ level: string; point: TradingPoint }>
  ): 'strong' | 'medium' | 'weak' {
    // 根据信号数量和置信度计算共振强度
    const highConfidenceCount = signals.filter(
      s => s.point.confidence === 'high'
    ).length;

    if (signals.length >= 3 && highConfidenceCount >= 2) {
      return 'strong';
    } else if (signals.length >= 2 && highConfidenceCount >= 1) {
      return 'medium';
    } else {
      return 'weak';
    }
  }
}
```

---

### 3.4 信号生成和推送

#### 功能需求

**FR-3.4.1 信号生成**
- 基于策略条件生成信号
- 信号的优先级排序
- 信号的去重和合并

**FR-3.4.2 信号推送**
- WebSocket 实时推送
- 消息队列异步推送
- 多种通知方式（邮件、短信、推送通知）

**FR-3.4.3 信号管理**
- 信号历史记录
- 信号的确认和忽略
- 信号的统计分析

#### 技术实现

```typescript
class SignalService {
  private activeStrategies = new Map<string, Strategy>();
  private subscribers = new Map<string, WebSocket[]>();

  async onNewChanResult(
    symbol: string,
    interval: string,
    chanResult: ChanResult
  ) {
    // 1. 收集该标的所有级别的计算结果
    const chanResults = await this.collectChanResults(symbol);

    // 2. 评估所有激活的策略
    for (const [strategyId, strategy] of this.activeStrategies) {
      if (!this.isApplicable(strategy, symbol)) {
        continue;
      }

      // 评估策略
      const signal = await this.strategyExecutor.evaluateStrategy(
        strategy,
        symbol,
        chanResults
      );

      if (signal) {
        // 3. 保存信号
        await this.saveSignal(signal);

        // 4. 推送信号
        await this.pushSignal(signal);

        // 5. 如果启用自动交易，执行交易
        if (strategy.autoTrade) {
          await this.executeAutoTrade(signal);
        }
      }
    }
  }

  private async pushSignal(signal: Signal) {
    // 推送到 WebSocket 客户端
    const subscribers = this.subscribers.get(signal.symbol) || [];
    const message = JSON.stringify({
      type: 'signal',
      data: signal
    });

    for (const ws of subscribers) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(message);
      }
    }

    // 推送到消息队列
    await this.messageQueue.publish('signals', signal);

    // 发送通知
    if (signal.confidence === 'high') {
      await this.notificationService.send({
        type: 'high-confidence-signal',
        title: `${signal.symbol} ${signal.type === 'buy' ? '买入' : '卖出'}信号`,
        body: signal.reason,
        data: signal
      });
    }
  }

  // WebSocket 订阅管理
  subscribeSignals(
    userId: string,
    symbols: string[],
    ws: WebSocket
  ) {
    for (const symbol of symbols) {
      if (!this.subscribers.has(symbol)) {
        this.subscribers.set(symbol, []);
      }
      this.subscribers.get(symbol)!.push(ws);
    }

    // 发送最近的信号
    this.sendRecentSignals(ws, symbols);
  }

  unsubscribeSignals(ws: WebSocket) {
    for (const [symbol, subs] of this.subscribers) {
      const index = subs.indexOf(ws);
      if (index !== -1) {
        subs.splice(index, 1);
      }
    }
  }

  private async sendRecentSignals(
    ws: WebSocket,
    symbols: string[]
  ) {
    const recentSignals = await this.db.query(
      'SELECT * FROM signals ' +
      'WHERE symbol IN (?) ' +
      'AND timestamp > ? ' +
      'ORDER BY timestamp DESC ' +
      'LIMIT 10',
      [symbols, Date.now() - 24 * 3600 * 1000]
    );

    ws.send(JSON.stringify({
      type: 'recent-signals',
      data: recentSignals
    }));
  }
}
```

---

### 3.5 策略性能统计

#### 功能需求

**FR-3.5.1 实时统计**
- 当日盈亏
- 本周盈亏
- 本月盈亏
- 累计盈亏

**FR-3.5.2 策略对比**
- 多个策略的性能对比
- 最佳策略推荐
- 策略组合优化

**FR-3.5.3 风险监控**
- 当前回撤
- 风险敞口
- 仓位分布
- 相关性分析

#### 技术实现

```typescript
class StrategyPerformanceTracker {
  async trackPerformance(
    strategyId: string
  ): Promise<PerformanceMetrics> {
    // 获取策略的所有交易记录
    const trades = await this.db.getTrades(strategyId);

    // 计算各项指标
    const metrics: PerformanceMetrics = {
      // 收益指标
      totalReturn: this.calculateTotalReturn(trades),
      dailyReturn: this.calculateDailyReturn(trades),
      weeklyReturn: this.calculateWeeklyReturn(trades),
      monthlyReturn: this.calculateMonthlyReturn(trades),

      // 风险指标
      maxDrawdown: this.calculateMaxDrawdown(trades),
      currentDrawdown: this.calculateCurrentDrawdown(trades),
      sharpeRatio: this.calculateSharpeRatio(trades),
      volatility: this.calculateVolatility(trades),

      // 交易指标
      totalTrades: trades.length,
      winRate: this.calculateWinRate(trades),
      profitFactor: this.calculateProfitFactor(trades),
      averageWin: this.calculateAverageWin(trades),
      averageLoss: this.calculateAverageLoss(trades),

      // 时间指标
      averageHoldingTime: this.calculateAverageHoldingTime(trades),
      tradingFrequency: this.calculateTradingFrequency(trades),

      // 更新时间
      updatedAt: Date.now()
    };

    return metrics;
  }

  async compareStrategies(
    strategyIds: string[]
  ): Promise<StrategyComparison> {
    const performances = await Promise.all(
      strategyIds.map(id => this.trackPerformance(id))
    );

    // 按不同维度排序
    const byReturn = [...performances].sort(
      (a, b) => b.totalReturn - a.totalReturn
    );
    const bySharpe = [...performances].sort(
      (a, b) => b.sharpeRatio - a.sharpeRatio
    );
    const byDrawdown = [...performances].sort(
      (a, b) => a.maxDrawdown - b.maxDrawdown
    );

    return {
      performances,
      rankings: {
        byReturn,
        bySharpe,
        byDrawdown
      },
      recommendation: this.recommendBestStrategy(performances)
    };
  }

  private recommendBestStrategy(
    performances: PerformanceMetrics[]
  ): string {
    // 综合评分：收益 * 0.4 + 夏普 * 0.3 - 回撤 * 0.3
    const scores = performances.map((p, index) => ({
      index,
      score: p.totalReturn * 0.4 +
             p.sharpeRatio * 0.3 -
             p.maxDrawdown * 0.3
    }));

    scores.sort((a, b) => b.score - a.score);
    return performances[scores[0].index].strategyId;
  }
}
```

---

## Phase 3 交付物

### 代码交付物
- [ ] 策略配置系统
- [ ] 回测引擎
- [ ] 多级别联立分析器
- [ ] 信号生成和推送服务
- [ ] 策略性能统计模块

### 测试交付物
- [ ] 策略配置测试
- [ ] 回测准确性测试
- [ ] 多级别分析测试
- [ ] 信号推送测试

### 文档交付物
- [ ] 策略配置指南
- [ ] 回测使用文档
- [ ] API 文档

---

## 验收标准

### 功能验收
- [ ] 可以配置和保存策略
- [ ] 回测引擎正常工作
- [ ] 回测结果准确
- [ ] 多级别分析正确
- [ ] 信号推送及时

### 性能验收
- [ ] 回测速度 < 10秒/年
- [ ] 信号推送延迟 < 1秒
- [ ] 支持10个策略并行运行

### 质量验收
- [ ] 回测结果与实盘一致
- [ ] 策略逻辑正确
- [ ] 无重大 bug

---

## 下一阶段

完成 Phase 3 后，进入 [Phase 4: 自动交易](./04-phase4-auto-trading.md)
