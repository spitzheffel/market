# Phase 1: 数据层 + 基础原型

## 阶段概述

**时间**: 3-4 周
**目标**: 建立稳定的数据基础设施，并实现基础的缠论计算原型
**交付物**: 可以实时看到分型和笔的系统

---

## 阶段目标

### 核心目标
1. ✅ 完整的数据源管理系统（支持多交易所）
2. ✅ 稳定的数据同步机制（实时 + 历史）
3. ✅ 数据完整性保障（检测 + 修复）
4. ✅ 基础的缠论计算（分型 + 笔）
5. ✅ 前端实时展示

### 为什么这样设计？

**数据层必须一次做对：**
- 数据是整个系统的基础
- 后续所有功能都依赖数据质量
- 数据层返工成本极高

**同时实现基础计算：**
- 快速看到效果，保持开发动力
- 验证数据格式是否正确
- 及早发现数据质量问题

---

## Week 1-2: 数据源管理 + 数据同步

### 1.1 数据源管理

#### 功能需求

**FR-1.1.1 多交易所适配**
- 支持 Binance、OKX、Bybit
- 统一的数据源接口
- 可配置的交易所参数

**FR-1.1.2 数据格式标准化**
- 统一的 Kline 数据结构
- 时间戳标准化（毫秒级 Unix 时间戳）
- 交易对命名标准化（BTC/USDT 格式）
- 价格精度处理（使用字符串避免浮点数问题）

**FR-1.1.3 API 配置管理**
- API Key/Secret 加密存储
- 请求限流配置
- 超时和重试策略
- 多环境配置（开发/测试/生产）

**FR-1.1.4 延迟监控**
- 记录每次 API 请求的延迟
- WebSocket 推送延迟监控
- 延迟超过阈值时告警
- 延迟统计和可视化

#### 技术实现

**数据源接口定义：**

```typescript
interface IDataSource {
  // 基础信息
  name: string;  // 'binance', 'okx', 'bybit'

  // 获取历史K线
  getKlines(
    symbol: string,
    interval: string,
    start: number,
    end: number
  ): Promise<Kline[]>;

  // 订阅实时K线
  subscribeKline(
    symbol: string,
    interval: string,
    callback: (kline: Kline) => void
  ): void;

  // 取消订阅
  unsubscribe(symbol: string, interval: string): void;

  // 健康检查
  healthCheck(): Promise<HealthStatus>;

  // 获取延迟统计
  getLatencyStats(): LatencyStats;
}
```

**统一数据格式：**

```typescript
interface Kline {
  symbol: string;        // 'BTC/USDT'
  interval: string;      // '1m', '5m', '15m', '1h', '4h', '1d'
  timestamp: number;     // 毫秒级 Unix 时间戳
  open: string;          // 开盘价（字符串）
  high: string;          // 最高价
  low: string;           // 最低价
  close: string;         // 收盘价
  volume: string;        // 成交量
  closed: boolean;       // K线是否已完成
}
```

**配置管理：**

```typescript
interface ExchangeConfig {
  name: string;
  enabled: boolean;

  apiKey: string;        // 加密存储
  apiSecret: string;     // 加密存储

  restApi: {
    baseUrl: string;
    timeout: number;     // 毫秒
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

  latencyMonitoring: {
    enabled: boolean;
    alertThreshold: number;  // 毫秒
  };
}
```

#### 验收标准

- [ ] 可以成功连接 Binance、OKX、Bybit
- [ ] 可以获取历史 K线数据
- [ ] 数据格式统一且正确
- [ ] API Key 加密存储
- [ ] 延迟监控正常工作
- [ ] 延迟超过阈值时能收到告警

---

### 1.2 历史数据回补

#### 功能需求

**FR-1.2.1 批量数据获取**
- 支持指定时间范围回补数据
- 自动分批请求（避免触发限流）
- 请求失败自动重试
- 回补进度可视化

**FR-1.2.2 数据存储**
- 使用 PostgreSQL + TimescaleDB 存储
- 自动创建时间分区
- 建立合适的索引
- 数据去重

**FR-1.2.3 回补任务管理**
- 支持多个回补任务并行
- 任务状态跟踪（pending/running/completed/failed）
- 失败任务自动重试
- 任务优先级管理

#### 技术实现

**回补服务：**

```typescript
class HistoricalDataBackfill {
  async backfill(
    symbol: string,
    interval: string,
    startTime: number,
    endTime: number
  ): Promise<BackfillResult> {
    const batchSize = 1000;
    const intervalMs = this.getIntervalMs(interval);

    // 计算需要多少批次
    const batches = this.calculateBatches(
      startTime,
      endTime,
      intervalMs,
      batchSize
    );

    // 使用限流器控制请求速度
    const limiter = new RateLimiter(10, 1000); // 每秒10个请求

    let successCount = 0;
    let failedBatches = [];

    for (const batch of batches) {
      await limiter.wait();

      try {
        const klines = await this.dataSource.getKlines(
          symbol,
          interval,
          batch.start,
          batch.end
        );

        // 保存到数据库
        await this.db.saveKlines(klines);
        successCount += klines.length;

        // 更新进度
        this.emit('progress', {
          symbol,
          interval,
          progress: batch.index / batches.length * 100,
          successCount
        });

      } catch (error) {
        console.error(`Batch failed:`, error);
        failedBatches.push({ batch, error });

        // 记录失败的批次，稍后重试
        await this.db.saveFailedBatch(batch, error);
      }
    }

    return {
      totalBatches: batches.length,
      successCount,
      failedCount: failedBatches.length,
      failedBatches
    };
  }
}
```

**数据库设计：**

```sql
-- K线数据表（使用 TimescaleDB）
CREATE TABLE klines (
  id BIGSERIAL,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  timestamp BIGINT NOT NULL,
  open DECIMAL(20, 8) NOT NULL,
  high DECIMAL(20, 8) NOT NULL,
  low DECIMAL(20, 8) NOT NULL,
  close DECIMAL(20, 8) NOT NULL,
  volume DECIMAL(20, 8) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  PRIMARY KEY (symbol, interval, timestamp)
);

-- 转换为时序表
SELECT create_hypertable('klines', 'timestamp',
  chunk_time_interval => 86400000000);  -- 1天

-- 创建索引
CREATE INDEX idx_klines_symbol_interval ON klines(symbol, interval);
CREATE INDEX idx_klines_timestamp ON klines(timestamp DESC);

-- 回补任务表
CREATE TABLE backfill_tasks (
  id SERIAL PRIMARY KEY,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  start_time BIGINT NOT NULL,
  end_time BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,  -- pending/running/completed/failed
  progress INT DEFAULT 0,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 验收标准

- [ ] 可以成功回补历史数据
- [ ] 回补速度符合预期（不触发限流）
- [ ] 数据正确保存到数据库
- [ ] 回补进度实时更新
- [ ] 失败的批次能自动重试
- [ ] 数据库查询性能良好

---

### 1.3 实时数据同步

#### 功能需求

**FR-1.3.1 WebSocket 连接管理**
- 自动连接和断线重连
- 心跳保活
- 连接状态监控
- 多连接管理（每个交易所可能需要多个连接）

**FR-1.3.2 实时数据处理**
- 接收 WebSocket 推送的 K线数据
- 区分完成的 K线和未完成的 K线
- 完成的 K线保存到数据库
- 未完成的 K线更新临时状态

**FR-1.3.3 数据推送到前端**
- 通过 WebSocket 推送到前端
- 支持多个前端客户端订阅
- 按标的和周期分组推送

#### 技术实现

**WebSocket 客户端：**

```typescript
class RealtimeDataSync {
  private ws: WebSocket;
  private subscriptions = new Map<string, Subscription>();
  private reconnectAttempts = 0;
  private heartbeatTimer: NodeJS.Timeout;

  async connect() {
    this.ws = new WebSocket(this.config.wsUrl);

    this.ws.on('open', () => {
      console.log('[WebSocket] Connected');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
      this.resubscribeAll();
    });

    this.ws.on('message', async (data) => {
      const message = JSON.parse(data);
      await this.handleMessage(message);
    });

    this.ws.on('close', () => {
      console.log('[WebSocket] Closed');
      this.stopHeartbeat();
      this.reconnect();
    });

    this.ws.on('error', (error) => {
      console.error('[WebSocket] Error:', error);
    });
  }

  private async handleMessage(message: any) {
    // 解析K线数据
    const kline = this.parseKline(message);

    if (kline.closed) {
      // K线已完成，保存到数据库
      await this.db.saveKline(kline);

      // 触发缠论计算
      this.emit('kline-closed', kline);

      // 推送到前端
      this.broadcastToFrontend('kline-closed', kline);

    } else {
      // K线未完成，更新临时数据
      this.updateTempKline(kline);

      // 推送到前端（用于实时显示）
      this.broadcastToFrontend('kline-update', kline);
    }
  }

  private reconnect() {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      console.error('[WebSocket] Max reconnect attempts reached');
      this.emit('fatal-error', new Error('Cannot reconnect'));
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempts),
      30000
    );

    console.log(`[WebSocket] Reconnecting in ${delay}ms...`);
    setTimeout(() => this.connect(), delay);
  }

  private startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws.readyState === WebSocket.OPEN) {
        this.ws.ping();
      }
    }, this.config.heartbeatInterval);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
    }
  }
}
```

#### 验收标准

- [ ] WebSocket 连接稳定
- [ ] 断线后能自动重连
- [ ] 心跳保活正常工作
- [ ] 能正确接收实时 K线数据
- [ ] 完成的 K线正确保存到数据库
- [ ] 前端能实时看到 K线更新

---

## Week 3-4: 基础缠论计算 + 前端集成

### 1.4 数据完整性检测和修复

#### 功能需求

**FR-1.4.1 缺失数据检测**
- 定期扫描数据库，检测缺失的 K线
- 按标的和周期分组检测
- 生成缺失数据报告

**FR-1.4.2 自动修复**
- 发现缺失数据后自动回补
- 回补完成后触发重新计算
- 修复记录和统计

**FR-1.4.3 数据质量监控**
- 监控数据完整性
- 监控数据延迟
- 异常数据告警

#### 技术实现

详见下一个文件...

#### 验收标准

- [ ] 能准确检测出缺失的 K线
- [ ] 缺失数据能自动回补
- [ ] 回补后能触发重新计算
- [ ] 数据质量监控正常工作
- [ ] 异常情况能及时告警

---

## 交付物清单

### 代码交付物
- [ ] 数据源适配器（Binance、OKX、Bybit）
- [ ] 历史数据回补服务
- [ ] 实时数据同步服务
- [ ] 数据完整性检测服务
- [ ] 基础缠论计算引擎（分型 + 笔）
- [ ] 后端 API 接口
- [ ] 前端图表展示组件

### 文档交付物
- [ ] 数据源接入文档
- [ ] API 文档
- [ ] 数据库设计文档
- [ ] 部署文档

### 测试交付物
- [ ] 单元测试（覆盖率 > 70%）
- [ ] 集成测试
- [ ] 性能测试报告

---

## 风险和应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 交易所 API 限流 | 高 | 实现限流器，控制请求速度 |
| WebSocket 不稳定 | 高 | 实现重连机制，多连接冗余 |
| 数据格式变化 | 中 | 适配器模式，版本管理 |
| 数据库性能 | 中 | 使用 TimescaleDB，优化索引 |

---

## 下一阶段

完成 Phase 1 后，进入 [Phase 2: 完善缠论计算](./02-phase2-chan-calculation.md)
