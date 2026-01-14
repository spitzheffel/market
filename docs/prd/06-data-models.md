# 数据模型设计

## 核心数据模型

### 1. K线数据 (Kline)

```typescript
interface Kline {
  symbol: string;        // 交易对，如 'BTC/USDT'
  interval: string;      // 周期，如 '1m', '5m', '15m'
  timestamp: number;     // 时间戳（毫秒）
  open: string;          // 开盘价
  high: string;          // 最高价
  low: string;           // 最低价
  close: string;         // 收盘价
  volume: string;        // 成交量
  closed: boolean;       // 是否已完成
}
```

**数据库表：**
```sql
CREATE TABLE klines (
  time TIMESTAMPTZ NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  open DECIMAL(20, 8) NOT NULL,
  high DECIMAL(20, 8) NOT NULL,
  low DECIMAL(20, 8) NOT NULL,
  close DECIMAL(20, 8) NOT NULL,
  volume DECIMAL(20, 8) NOT NULL,
  PRIMARY KEY (symbol, interval, time)
);

SELECT create_hypertable('klines', 'time');
CREATE INDEX idx_klines_symbol_interval ON klines(symbol, interval, time DESC);
```

---

### 2. 缠论数据模型

#### 2.1 处理后的K线 (MergedKline)

```typescript
interface MergedKline {
  index: number;           // 在原始K线序列中的索引
  direction: 'up' | 'down' | null;
  high: string;
  low: string;
  open: string;
  close: string;
  timestamp: number;
  originalKlines: number[];  // 包含的原始K线索引
}
```

#### 2.2 分型 (Fenxing)

```typescript
interface Fenxing {
  id: string;
  centerIndex: number;     // 中间K线索引
  leftIndex: number;       // 左侧K线索引
  rightIndex: number;      // 右侧K线索引
  type: 'top' | 'bottom';
  price: string;           // 分型价格
  timestamp: number;
  confirmed: boolean;      // 是否被后续K线确认
}
```

#### 2.3 笔 (Bi)

```typescript
interface Bi {
  id: string;
  startFenxing: Fenxing;
  endFenxing: Fenxing;
  direction: 'up' | 'down';
  klineCount: number;      // 包含的K线数量
  startPrice: string;
  endPrice: string;
  startTime: number;
  endTime: number;
  confirmed: boolean;
}
```

#### 2.4 线段 (Xianduan)

```typescript
interface Xianduan {
  id: string;
  startBi: Bi;
  endBi: Bi;
  biList: Bi[];            // 包含的所有笔
  direction: 'up' | 'down';
  startPrice: string;
  endPrice: string;
  startTime: number;
  endTime: number;
  confirmed: boolean;
  broken: boolean;         // 是否被破坏
  tezhengSequence: TezhengElement[];
}

interface TezhengElement {
  index: number;
  bi: Bi;
  price: string;
  timestamp: number;
  type: 'top' | 'bottom';
}
```

#### 2.5 中枢 (Zhongshu)

```typescript
interface Zhongshu {
  id: string;
  level: 'bi' | 'xianduan';
  components: Bi[] | Xianduan[];
  high: string;            // 上轨
  low: string;             // 下轨
  center: string;          // 中轨
  startTime: number;
  endTime: number;
  type: 'extending' | 'new' | 'moving';
  oscillations: number;    // 震荡次数
  confirmed: boolean;
}
```

#### 2.6 买卖点 (TradingPoint)

```typescript
interface TradingPoint {
  id: string;
  type: 'buy' | 'sell';
  level: 1 | 2 | 3;
  index: number;
  price: string;
  timestamp: number;
  reason: string;
  confidence: 'high' | 'medium' | 'low';
  divergence?: {
    macd: number;
    volume: number;
    type: 'bullish' | 'bearish';
  };
  relatedStructures: {
    bi?: Bi;
    xianduan?: Xianduan;
    zhongshu?: Zhongshu;
  };
}
```

#### 2.7 缠论计算结果 (ChanResult)

```typescript
interface ChanResult {
  symbol: string;
  interval: string;
  klines: Kline[];
  mergedKlines: MergedKline[];
  fenxing: Fenxing[];
  bi: Bi[];
  xianduan: Xianduan[];
  zhongshu: Zhongshu[];
  tradingPoints: TradingPoint[];
  calculatedAt: number;
}
```

**数据库表：**
```sql
CREATE TABLE chan_results (
  time TIMESTAMPTZ NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  result JSONB NOT NULL,
  PRIMARY KEY (symbol, interval, time)
);

SELECT create_hypertable('chan_results', 'time');
CREATE INDEX idx_chan_results_symbol_interval ON chan_results(symbol, interval, time DESC);
```

---

### 3. 策略数据模型

#### 3.1 策略 (Strategy)

```typescript
interface Strategy {
  id: string;
  name: string;
  description: string;
  version: string;
  levels: string[];
  entryConditions: {
    primary: LevelCondition;
    secondary?: LevelCondition;
    confirmation?: LevelCondition;
  };
  exitConditions: {
    stopLoss: StopLossConfig;
    takeProfit: TakeProfitConfig;
    trailingStop?: TrailingStopConfig;
    timeStop?: TimeStopConfig;
  };
  positionSizing: PositionSizingConfig;
  riskControl: RiskControlConfig;
  autoTrade: boolean;
  enabled: boolean;
  createdAt: number;
  updatedAt: number;
}
```

**数据库表：**
```sql
CREATE TABLE strategies (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  version VARCHAR(20),
  config JSONB NOT NULL,
  auto_trade BOOLEAN DEFAULT FALSE,
  enabled BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_strategies_enabled ON strategies(enabled);
```

#### 3.2 信号 (Signal)

```typescript
interface Signal {
  id: string;
  strategyId: string;
  symbol: string;
  type: 'buy' | 'sell';
  entryPrice: string;
  stopLoss: string;
  takeProfit: string;
  positionSize: number;
  confidence: 'high' | 'medium' | 'low';
  reason: string;
  timestamp: number;
  status: 'pending' | 'executed' | 'ignored' | 'expired';
}
```

**数据库表：**
```sql
CREATE TABLE signals (
  id VARCHAR(50) PRIMARY KEY,
  strategy_id VARCHAR(50) NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  type VARCHAR(10) NOT NULL,
  entry_price DECIMAL(20, 8) NOT NULL,
  stop_loss DECIMAL(20, 8),
  take_profit DECIMAL(20, 8),
  position_size DECIMAL(20, 8),
  confidence VARCHAR(10),
  reason TEXT,
  timestamp BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (strategy_id) REFERENCES strategies(id)
);

CREATE INDEX idx_signals_strategy ON signals(strategy_id);
CREATE INDEX idx_signals_symbol ON signals(symbol);
CREATE INDEX idx_signals_timestamp ON signals(timestamp DESC);
```

---

### 4. 交易数据模型

#### 4.1 订单 (Order)

```typescript
interface Order {
  id: string;
  exchangeOrderId?: string;
  symbol: string;
  type: 'market' | 'limit' | 'stop' | 'stop_limit';
  side: 'buy' | 'sell';
  size: number;
  price?: string;
  stopPrice?: string;
  status: 'pending' | 'submitted' | 'partial_filled' | 'filled' | 'cancelled' | 'failed';
  filledSize?: number;
  averagePrice?: string;
  fee?: number;
  error?: string;
  createdAt: number;
  updatedAt: number;
}
```

**数据库表：**
```sql
CREATE TABLE orders (
  id VARCHAR(50) PRIMARY KEY,
  exchange_order_id VARCHAR(100),
  symbol VARCHAR(20) NOT NULL,
  type VARCHAR(20) NOT NULL,
  side VARCHAR(10) NOT NULL,
  size DECIMAL(20, 8) NOT NULL,
  price DECIMAL(20, 8),
  stop_price DECIMAL(20, 8),
  status VARCHAR(20) NOT NULL,
  filled_size DECIMAL(20, 8),
  average_price DECIMAL(20, 8),
  fee DECIMAL(20, 8),
  error TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
```

#### 4.2 持仓 (Position)

```typescript
interface Position {
  id: string;
  orderId: string;
  symbol: string;
  side: 'long' | 'short';
  entryPrice: string;
  entryTime: number;
  exitPrice?: string;
  exitTime?: number;
  size: number;
  currentPrice: string;
  stopLoss: string;
  takeProfit?: string;
  trailingStop?: TrailingStopConfig;
  unrealizedPnL: number;
  realizedPnL: number;
  status: 'open' | 'closed';
  closeReason?: string;
  strategyId?: string;
  signalId?: string;
}
```

**数据库表：**
```sql
CREATE TABLE positions (
  id VARCHAR(50) PRIMARY KEY,
  order_id VARCHAR(50) NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  side VARCHAR(10) NOT NULL,
  entry_price DECIMAL(20, 8) NOT NULL,
  entry_time BIGINT NOT NULL,
  exit_price DECIMAL(20, 8),
  exit_time BIGINT,
  size DECIMAL(20, 8) NOT NULL,
  current_price DECIMAL(20, 8),
  stop_loss DECIMAL(20, 8),
  take_profit DECIMAL(20, 8),
  unrealized_pnl DECIMAL(20, 8),
  realized_pnl DECIMAL(20, 8),
  status VARCHAR(20) DEFAULT 'open',
  close_reason VARCHAR(50),
  strategy_id VARCHAR(50),
  signal_id VARCHAR(50),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (strategy_id) REFERENCES strategies(id),
  FOREIGN KEY (signal_id) REFERENCES signals(id)
);

CREATE INDEX idx_positions_symbol ON positions(symbol);
CREATE INDEX idx_positions_status ON positions(status);
CREATE INDEX idx_positions_strategy ON positions(strategy_id);
```

#### 4.3 账户 (Account)

```typescript
interface Account {
  userId: string;
  exchange: string;
  totalEquity: number;
  availableBalance: number;
  usedMargin: number;
  unrealizedPnL: number;
  marginLevel: number;
  positions: Position[];
  openOrders: number;
  updatedAt: number;
}
```

**数据库表：**
```sql
CREATE TABLE accounts (
  user_id VARCHAR(50) NOT NULL,
  exchange VARCHAR(20) NOT NULL,
  total_equity DECIMAL(20, 8) NOT NULL,
  available_balance DECIMAL(20, 8) NOT NULL,
  used_margin DECIMAL(20, 8) NOT NULL,
  unrealized_pnl DECIMAL(20, 8),
  margin_level DECIMAL(10, 2),
  updated_at TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (user_id, exchange)
);
```

---

### 5. 配置数据模型

#### 5.1 交易所配置 (ExchangeConfig)

```typescript
interface ExchangeConfig {
  name: string;
  enabled: boolean;
  apiKey: string;
  apiSecret: string;
  restApi: {
    baseUrl: string;
    timeout: number;
    rateLimit: {
      requestsPerSecond: number;
      requestsPerMinute: number;
    };
  };
  websocket: {
    url: string;
    maxConnections: number;
    heartbeatInterval: number;
    reconnectStrategy: {
      maxAttempts: number;
      backoffMultiplier: number;
      maxDelay: number;
    };
  };
}
```

**数据库表：**
```sql
CREATE TABLE exchange_configs (
  name VARCHAR(20) PRIMARY KEY,
  enabled BOOLEAN DEFAULT TRUE,
  api_key TEXT NOT NULL,
  api_secret TEXT NOT NULL,
  config JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 5.2 风控配置 (RiskConfig)

```typescript
interface RiskConfig {
  maxRiskPerTrade: number;
  maxPositionSize: number;
  maxTotalExposure: number;
  maxDailyLoss: number;
  maxDrawdown: number;
  maxPositions: number;
  maxConsecutiveLosses: number;
  cooldownAfterLoss: number;
  maxCorrelation: number;
  closeAllOnCircuitBreaker: boolean;
}
```

**数据库表：**
```sql
CREATE TABLE risk_configs (
  id SERIAL PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  config JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

---

### 6. 日志数据模型

#### 6.1 交易日志 (TradeLog)

```typescript
interface TradeLog {
  id: string;
  type: 'signal' | 'order' | 'position' | 'risk_check';
  level: 'info' | 'warning' | 'error';
  message: string;
  data: any;
  timestamp: number;
}
```

**数据库表：**
```sql
CREATE TABLE trade_logs (
  id SERIAL PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  level VARCHAR(10) NOT NULL,
  message TEXT NOT NULL,
  data JSONB,
  timestamp BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_trade_logs_type ON trade_logs(type);
CREATE INDEX idx_trade_logs_timestamp ON trade_logs(timestamp DESC);
```

---

## 数据关系图

```
User
  ├── Account
  ├── Strategy (1:N)
  │     ├── Signal (1:N)
  │     └── Position (1:N)
  └── RiskConfig (1:1)

Signal
  ├── Order (1:1)
  └── Position (0:1)

Order
  └── Position (0:1)

Position
  ├── Order (N:1)
  ├── Signal (N:1)
  └── Strategy (N:1)

Kline
  └── ChanResult (1:1)

ChanResult
  ├── Fenxing (1:N)
  ├── Bi (1:N)
  ├── Xianduan (1:N)
  ├── Zhongshu (1:N)
  └── TradingPoint (1:N)
```

---

## 数据迁移策略

### 版本管理

使用 Flyway 进行数据库版本管理：

```
db/migration/
├── V1__initial_schema.sql
├── V2__add_strategy_tables.sql
├── V3__add_trading_tables.sql
└── V4__add_indexes.sql
```

### 数据备份

- **每日全量备份**: 凌晨 2:00
- **每小时增量备份**: 整点执行
- **备份保留**: 30 天
- **异地备份**: 同步到云存储

---

## 下一步

查看 [验收标准和测试计划](./07-acceptance-criteria.md)
