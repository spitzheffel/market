# Phase 3: 策略和回测

## 阶段概述

**时间**: 3-4 周
**目标**: 实现策略配置系统和回测引擎
**交付物**: 可以验证策略有效性的完整系统

---

## 阶段目标

### 核心目标
1. ✅ 实现策略配置系统
2. ✅ 实现回测引擎
3. ✅ 实现多级别联立分析
4. ✅ 实现信号生成和推送
5. ✅ 实现策略性能统计

---

## Week 9-10: 策略配置系统 + 回测引擎

### 3.1 策略配置系统

#### 功能需求

**FR-3.1.1 策略定义**
- 策略名称和描述
- 适用的级别（1m/5m/15m等）
- 入场条件配置
- 出场条件配置
- 仓位管理配置
- 风控参数配置

**FR-3.1.2 条件构建器**
- 可视化的条件配置界面
- 支持多种条件类型（买卖点、趋势、中枢位置、背驰等）
- 条件的逻辑组合（AND/OR）
- 条件的优先级

**FR-3.1.3 策略模板**
- 预定义的策略模板
- 用户自定义策略
- 策略的导入导出
- 策略的版本管理

#### 数据模型

```typescript
interface Strategy {
  id: string;
  name: string;
  description: string;
  version: string;

  // 适用级别
  levels: string[];  // ['1m', '5m', '15m']

  // 入场条件
  entryConditions: {
    primary: LevelCondition;      // 主级别条件
    secondary?: LevelCondition;   // 辅助级别条件
    confirmation?: LevelCondition; // 确认级别条件
  };

  // 出场条件
  exitConditions: {
    stopLoss: StopLossConfig;
    takeProfit: TakeProfitConfig;
    trailingStop?: TrailingStopConfig;
    timeStop?: TimeStopConfig;
  };

  // 仓位管理
  positionSizing: PositionSizingConfig;

  // 风控参数
  riskControl: RiskControlConfig;

  // 是否启用自动交易
  autoTrade: boolean;

  // 创建和更新时间
  createdAt: number;
  updatedAt: number;
}

interface LevelCondition {
  level: string;  // '1m', '5m', '15m'
  conditions: Condition[];
  logic: 'AND' | 'OR';  // 条件之间的逻辑关系
}

type Condition =
  | TradingPointCondition
  | TrendCondition
  | ZhongshuPositionCondition
  | DivergenceCondition
  | CustomCondition;

interface TradingPointCondition {
  type: 'trading-point';
  level: 1 | 2 | 3;
  direction: 'buy' | 'sell';
  minConfidence?: 'high' | 'medium' | 'low';
}

interface TrendCondition {
  type: 'trend';
  direction: 'up' | 'down';
  minXianduanCount?: number;  // 至少几个线段
}

interface ZhongshuPositionCondition {
  type: 'zhongshu-position';
  position: 'inside' | 'above' | 'below';
  breakout?: boolean;  // 是否突破
}

interface DivergenceCondition {
  type: 'divergence';
  indicator: 'macd' | 'volume';
  divergenceType: 'bullish' | 'bearish';
  minStrength?: 'strong' | 'medium' | 'weak';
}

interface StopLossConfig {
  type: 'fixed' | 'fenxing' | 'atr' | 'zhongshu';
  value?: number;      // 固定止损百分比
  offset?: number;     // 偏移量
  atrMultiplier?: number;  // ATR倍数
}

interface TakeProfitConfig {
  type: 'fixed' | 'ratio' | 'target';
  value?: number;      // 固定止盈百分比
  ratio?: number;      // 盈亏比
  targetPrice?: string;
}

interface PositionSizingConfig {
  type: 'fixed' | 'fixed-risk' | 'kelly';
  value?: number;           // 固定仓位百分比
  riskPerTrade?: number;    // 每笔交易风险百分比
}

interface RiskControlConfig {
  maxDailyLoss: number;     // 单日最大亏损
  maxDrawdown: number;      // 最大回撤
  maxPositions: number;     // 最大持仓数
  cooldownAfterLoss?: number;  // 亏损后冷静期（毫秒）
}
```

#### 策略示例

```typescript
// 多级别共振策略
const multiLevelStrategy: Strategy = {
  id: 'multi-level-resonance',
  name: '多级别共振策略',
  description: '小级别买点 + 中级别上涨趋势 + 大级别突破中枢',
  version: '1.0.0',
  levels: ['1m', '5m', '15m'],

  entryConditions: {
    primary: {
      level: '1m',
      conditions: [
        {
          type: 'trading-point',
          level: 1,
          direction: 'buy',
          minConfidence: 'medium'
        }
      ],
      logic: 'AND'
    },
    secondary: {
      level: '5m',
      conditions: [
        {
          type: 'trend',
          direction: 'up',
          minXianduanCount: 2
        },
        {
          type: 'zhongshu-position',
          position: 'above'
        }
      ],
      logic: 'AND'
    },
    confirmation: {
      level: '15m',
      conditions: [
        {
          type: 'trend',
          direction: 'up'
        },
        {
          type: 'divergence',
          indicator: 'macd',
          divergenceType: 'bullish',
          minStrength: 'medium'
        }
      ],
      logic: 'AND'
    }
  },

  exitConditions: {
    stopLoss: {
      type: 'fenxing',
      offset: -0.005  // 在分型下方0.5%
    },
    takeProfit: {
      type: 'ratio',
      ratio: 2  // 盈亏比2:1
    },
    trailingStop: {
      enabled: true,
      activationRatio: 1.5,
      trailingRatio: 0.5
    }
  },

  positionSizing: {
    type: 'fixed-risk',
    riskPerTrade: 0.02  // 每笔交易风险2%
  },

  riskControl: {
    maxDailyLoss: 0.05,
    maxDrawdown: 0.15,
    maxPositions: 3,
    cooldownAfterLoss: 3600000  // 1小时
  },

  autoTrade: false,
  createdAt: Date.now(),
  updatedAt: Date.now()
};
```

---

### 3.2 回测引擎

#### 功能需求

**FR-3.2.1 历史数据回放**
- 按时间顺序回放历史K线
- 模拟实时计算过程
- 支持多标的同时回测

**FR-3.2.2 订单模拟**
- 模拟市价单执行
- 模拟限价单执行
- 模拟滑点
- 模拟手续费

**FR-3.2.3 性能统计**
- 总收益率
- 年化收益率
- 最大回撤
- 夏普比率
- 胜率和盈亏比
- 交易次数和频率
- 平均持仓时间

**FR-3.2.4 可视化报告**
- 权益曲线
- 回撤曲线
- 交易分布
- 月度收益热力图

#### 技术实现

```typescript
class BacktestEngine {
  async runBacktest(
    strategy: Strategy,
    symbols: string[],
    startTime: number,
    endTime: number,
    initialCapital: number
  ): Promise<BacktestResult> {
    // 初始化回测状态
    const state: BacktestState = {
      capital: initialCapital,
      equity: initialCapital,
      positions: [],
      orders: [],
      trades: [],
      equityCurve: [],
      peakEquity: initialCapital,
      maxDrawdown: 0,
      currentTime: startTime
    };

    // 加载历史数据
    const historicalData = await this.loadHistoricalData(
      symbols,
      strategy.levels,
      startTime,
      endTime
    );

    // 构建时间线
    const timeline = this.buildTimeline(historicalData);

    // 按时间顺序回放
    for (const event of timeline) {
      state.currentTime = event.timestamp;

      // 更新持仓盈亏
      this.updatePositions(state, event);

      // 检查出场条件
      await this.checkExitConditions(state, event);

      // 评估策略
      const signal = await this.strategyExecutor.evaluateStrategy(
        strategy,
        event.symbol,
        event.chanResults
      );

      if (signal) {
        // 模拟订单执行
        await this.simulateOrderExecution(state, signal, event);
      }

      // 记录权益曲线
      state.equityCurve.push({
        timestamp: event.timestamp,
        equity: state.equity,
        drawdown: this.calculateDrawdown(state)
      });

      // 更新最大回撤
      state.peakEquity = Math.max(state.peakEquity, state.equity);
      const currentDrawdown = (state.peakEquity - state.equity) / state.peakEquity;
      state.maxDrawdown = Math.max(state.maxDrawdown, currentDrawdown);
    }

    // 生成回测报告
    return this.generateReport(state, startTime, endTime);
  }

  private async simulateOrderExecution(
    state: BacktestState,
    signal: Signal,
    event: TimelineEvent
  ) {
    // 模拟滑点
    const slippage = this.calculateSlippage(
      signal.positionSize,
      event.marketDepth
    );
    const executionPrice = parseFloat(signal.entryPrice) * (1 + slippage);

    // 模拟手续费
    const fee = this.calculateFee(
      signal.positionSize,
      executionPrice
    );

    // 检查资金是否足够
    const requiredCapital = signal.positionSize * executionPrice + fee;
    if (requiredCapital > state.capital) {
      console.log('Insufficient capital');
      return;
    }

    // 创建持仓
    const position: Position = {
      id: this.generateId(),
      symbol: signal.symbol,
      entryPrice: executionPrice.toString(),
      entryTime: event.timestamp,
      size: signal.positionSize,
      stopLoss: signal.stopLoss,
      takeProfit: signal.takeProfit,
      unrealizedPnL: 0,
      realizedPnL: 0
    };

    state.positions.push(position);
    state.capital -= requiredCapital;

    // 记录交易
    state.trades.push({
      type: 'open',
      positionId: position.id,
      price: executionPrice.toString(),
      size: signal.positionSize,
      fee,
      timestamp: event.timestamp
    });
  }

  private generateReport(
    state: BacktestState,
    startTime: number,
    endTime: number
  ): BacktestResult {
    const totalReturn = (state.equity / state.initialCapital - 1) * 100;
    const trades = state.trades.filter(t => t.type === 'close');
    const winningTrades = trades.filter(t => t.pnl! > 0);
    const losingTrades = trades.filter(t => t.pnl! <= 0);

    const durationDays = (endTime - startTime) / (24 * 3600 * 1000);
    const annualizedReturn = (Math.pow(1 + totalReturn / 100, 365 / durationDays) - 1) * 100;

    return {
      // 基础指标
      totalReturn,
      annualizedReturn,
      maxDrawdown: state.maxDrawdown * 100,

      // 风险指标
      sharpeRatio: this.calculateSharpeRatio(state),
      sortinoRatio: this.calculateSortinoRatio(state),
      calmarRatio: annualizedReturn / (state.maxDrawdown * 100),

      // 交易统计
      totalTrades: trades.length,
      winningTrades: winningTrades.length,
      losingTrades: losingTrades.length,
      winRate: trades.length > 0 ? winningTrades.length / trades.length * 100 : 0,

      // 盈亏统计
      averageWin: this.average(winningTrades.map(t => t.pnl!)),
      averageLoss: this.average(losingTrades.map(t => t.pnl!)),
      profitFactor: this.calculateProfitFactor(trades),

      // 持仓统计
      averageHoldingTime: this.calculateAverageHoldingTime(trades),
      maxConsecutiveLosses: this.calculateMaxConsecutiveLosses(trades),

      // 详细数据
      equityCurve: state.equityCurve,
      trades: state.trades,
      monthlyReturns: this.calculateMonthlyReturns(state)
    };
  }
}
```

继续下一部分...
