# 技术架构设计

## 系统架构概览

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端层 (Frontend)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 市场扫描  │  │ 图表分析  │  │ 策略管理  │  │ 自动交易  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│         Vue 3 + Vite + ECharts + WebSocket                      │
└─────────────────────────────────────────────────────────────────┘
                              ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────────┐
│                        API 网关层 (Gateway)                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Spring Cloud Gateway / Nginx                             │  │
│  │  - 路由转发  - 负载均衡  - 限流  - 认证授权               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                        应用服务层 (Services)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 数据服务  │  │ 计算服务  │  │ 策略服务  │  │ 交易服务  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│         Spring Boot 4.0.1 + Java 21                             │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                        数据层 (Data Layer)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │PostgreSQL│  │TimescaleDB│ │  Redis   │  │  Kafka   │        │
│  │  主数据   │  │  时序数据  │  │  缓存    │  │ 消息队列  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                      外部服务层 (External)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ Binance  │  │   OKX    │  │  Bybit   │                      │
│  │   API    │  │   API    │  │   API    │                      │
│  └──────────┘  └──────────┘  └──────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 服务拆分

### 1. 数据服务 (Data Service)

**职责：**
- 管理多个交易所的数据源
- 实时数据同步
- 历史数据回补
- 数据完整性检测和修复

**技术栈：**
- Spring Boot
- WebSocket Client
- REST Client
- PostgreSQL + TimescaleDB

**关键接口：**
```java
@RestController
@RequestMapping("/api/data")
public class DataController {

    // 获取K线数据
    @GetMapping("/klines")
    public List<Kline> getKlines(
        @RequestParam String symbol,
        @RequestParam String interval,
        @RequestParam Long startTime,
        @RequestParam Long endTime
    );

    // 订阅实时数据
    @MessageMapping("/subscribe")
    public void subscribe(SubscribeRequest request);

    // 触发历史数据回补
    @PostMapping("/backfill")
    public BackfillTask backfill(@RequestBody BackfillRequest request);

    // 获取数据质量报告
    @GetMapping("/quality")
    public DataQualityReport getQualityReport(
        @RequestParam String symbol,
        @RequestParam String interval
    );
}
```

---

### 2. 计算服务 (Calculation Service)

**职责：**
- 缠论算法计算
- 增量计算优化
- 计算结果缓存
- 性能监控

**技术栈：**
- Spring Boot
- Redis (缓存)
- 多线程并行计算

**关键接口：**
```java
@RestController
@RequestMapping("/api/calculation")
public class CalculationController {

    // 计算缠论结构
    @PostMapping("/chan")
    public ChanResult calculate(@RequestBody CalculateRequest request);

    // 获取计算结果
    @GetMapping("/chan/{symbol}/{interval}")
    public ChanResult getResult(
        @PathVariable String symbol,
        @PathVariable String interval
    );

    // 批量计算
    @PostMapping("/chan/batch")
    public Map<String, ChanResult> batchCalculate(
        @RequestBody BatchCalculateRequest request
    );

    // 获取性能统计
    @GetMapping("/performance")
    public PerformanceStats getPerformanceStats();
}
```

---

### 3. 策略服务 (Strategy Service)

**职责：**
- 策略配置管理
- 策略评估执行
- 回测引擎
- 信号生成

**技术栈：**
- Spring Boot
- PostgreSQL
- Redis

**关键接口：**
```java
@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    // 创建策略
    @PostMapping
    public Strategy createStrategy(@RequestBody Strategy strategy);

    // 更新策略
    @PutMapping("/{id}")
    public Strategy updateStrategy(
        @PathVariable String id,
        @RequestBody Strategy strategy
    );

    // 运行回测
    @PostMapping("/{id}/backtest")
    public BacktestResult runBacktest(
        @PathVariable String id,
        @RequestBody BacktestRequest request
    );

    // 启用/禁用策略
    @PutMapping("/{id}/toggle")
    public void toggleStrategy(@PathVariable String id);

    // 获取策略性能
    @GetMapping("/{id}/performance")
    public StrategyPerformance getPerformance(@PathVariable String id);
}
```

---

### 4. 交易服务 (Trading Service)

**职责：**
- 订单管理
- 持仓管理
- 风控检查
- 自动交易执行

**技术栈：**
- Spring Boot
- PostgreSQL
- Redis
- 交易所 API

**关键接口：**
```java
@RestController
@RequestMapping("/api/trading")
public class TradingController {

    // 提交订单
    @PostMapping("/orders")
    public Order submitOrder(@RequestBody OrderRequest request);

    // 取消订单
    @DeleteMapping("/orders/{id}")
    public void cancelOrder(@PathVariable String id);

    // 获取持仓
    @GetMapping("/positions")
    public List<Position> getPositions();

    // 平仓
    @PostMapping("/positions/{id}/close")
    public void closePosition(@PathVariable String id);

    // 获取账户信息
    @GetMapping("/account")
    public Account getAccount();

    // 风控检查
    @PostMapping("/risk-check")
    public RiskCheckResult checkRisk(@RequestBody RiskCheckRequest request);
}
```

---

## 数据流设计

### 实时数据流

```
交易所 WebSocket
    ↓
数据服务 (接收并标准化)
    ↓
保存到 TimescaleDB
    ↓
发布到 Kafka (可选)
    ↓
计算服务 (增量计算)
    ↓
更新 Redis 缓存
    ↓
策略服务 (评估策略)
    ↓
生成信号
    ↓
WebSocket 推送到前端
    ↓
(如果启用自动交易) 交易服务执行
```

### 历史数据回补流

```
用户触发回补请求
    ↓
数据服务创建回补任务
    ↓
分批从交易所 API 获取数据
    ↓
保存到 TimescaleDB
    ↓
检测数据完整性
    ↓
触发计算服务重新计算
    ↓
更新缓存
```

---

## 缓存策略

### Redis 缓存设计

**1. 计算结果缓存**
```
Key: chan:{symbol}:{interval}
Value: ChanResult (JSON)
TTL: 1小时
```

**2. 实时价格缓存**
```
Key: price:{symbol}
Value: CurrentPrice (JSON)
TTL: 10秒
```

**3. 策略状态缓存**
```
Key: strategy:{id}:state
Value: StrategyState (JSON)
TTL: 永久 (手动失效)
```

**4. 用户会话缓存**
```
Key: session:{userId}
Value: UserSession (JSON)
TTL: 24小时
```

### 缓存更新策略

- **主动更新**: 计算完成后立即更新缓存
- **被动失效**: 数据变化时删除相关缓存
- **定时刷新**: 每小时刷新一次长期缓存

---

## 消息队列设计

### Kafka Topic 设计

**1. kline-events**
- 用途: K线数据更新事件
- 分区: 按 symbol 分区
- 消费者: 计算服务

**2. calculation-results**
- 用途: 计算结果事件
- 分区: 按 symbol 分区
- 消费者: 策略服务、前端推送服务

**3. signals**
- 用途: 交易信号事件
- 分区: 按 strategyId 分区
- 消费者: 交易服务、通知服务

**4. trades**
- 用途: 交易执行事件
- 分区: 按 userId 分区
- 消费者: 统计服务、审计服务

---

## 数据库设计

### PostgreSQL 表结构

**核心表：**
- `users` - 用户表
- `strategies` - 策略表
- `signals` - 信号表
- `orders` - 订单表
- `positions` - 持仓表
- `trades` - 交易记录表

**配置表：**
- `exchange_configs` - 交易所配置
- `risk_configs` - 风控配置
- `alert_rules` - 告警规则

**日志表：**
- `calculation_logs` - 计算日志
- `trade_logs` - 交易日志
- `error_logs` - 错误日志

### TimescaleDB 时序表

**K线数据表：**
```sql
CREATE TABLE klines (
  time TIMESTAMPTZ NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  open DECIMAL(20, 8) NOT NULL,
  high DECIMAL(20, 8) NOT NULL,
  low DECIMAL(20, 8) NOT NULL,
  close DECIMAL(20, 8) NOT NULL,
  volume DECIMAL(20, 8) NOT NULL
);

SELECT create_hypertable('klines', 'time');

CREATE INDEX idx_klines_symbol_interval ON klines(symbol, interval, time DESC);
```

**缠论计算结果表：**
```sql
CREATE TABLE chan_results (
  time TIMESTAMPTZ NOT NULL,
  symbol VARCHAR(20) NOT NULL,
  interval VARCHAR(10) NOT NULL,
  result JSONB NOT NULL
);

SELECT create_hypertable('chan_results', 'time');
```

---

## 性能优化策略

### 1. 计算性能优化

- **增量计算**: 只计算新增的K线影响的部分
- **并行计算**: 多标的并行计算
- **缓存复用**: 缓存中间计算结果
- **异步处理**: 非关键计算异步执行

### 2. 数据库性能优化

- **分区表**: 按时间分区
- **索引优化**: 为常用查询建立索引
- **连接池**: 使用 HikariCP 连接池
- **读写分离**: 主从复制，读写分离

### 3. 网络性能优化

- **HTTP/2**: 使用 HTTP/2 协议
- **压缩**: 启用 gzip 压缩
- **CDN**: 静态资源使用 CDN
- **WebSocket**: 实时数据使用 WebSocket

### 4. 前端性能优化

- **虚拟滚动**: 大列表使用虚拟滚动
- **懒加载**: 组件和路由懒加载
- **代码分割**: 按路由分割代码
- **缓存策略**: 合理使用浏览器缓存

---

## 安全设计

### 1. 认证授权

- **JWT Token**: 使用 JWT 进行身份认证
- **RBAC**: 基于角色的访问控制
- **API Key**: 交易所 API Key 加密存储

### 2. 数据安全

- **加密传输**: HTTPS/WSS
- **敏感数据加密**: AES-256 加密
- **SQL 注入防护**: 使用参数化查询
- **XSS 防护**: 输入验证和输出转义

### 3. 交易安全

- **二次确认**: 重要操作需要二次确认
- **IP 白名单**: 限制 API 访问 IP
- **操作审计**: 记录所有操作日志
- **异常检测**: 检测异常交易行为

---

## 监控和运维

### 1. 监控指标

**系统指标：**
- CPU 使用率
- 内存使用率
- 磁盘 I/O
- 网络流量

**应用指标：**
- 请求 QPS
- 响应时间
- 错误率
- 数据延迟

**业务指标：**
- 活跃用户数
- 交易笔数
- 盈亏统计
- 策略性能

### 2. 日志管理

- **日志级别**: DEBUG/INFO/WARN/ERROR
- **日志格式**: JSON 格式
- **日志收集**: Filebeat → Logstash → Elasticsearch
- **日志查询**: Kibana

### 3. 告警配置

- **Prometheus**: 指标采集
- **Grafana**: 可视化监控
- **AlertManager**: 告警管理
- **多渠道通知**: 邮件/短信/钉钉/企业微信

---

## 部署架构

### 开发环境

```
Docker Compose
├── frontend (Vue 3)
├── backend (Spring Boot)
├── postgresql
├── redis
└── kafka (可选)
```

### 生产环境

```
Kubernetes Cluster
├── Ingress (Nginx)
├── Frontend Pods (3 replicas)
├── Backend Pods
│   ├── Data Service (2 replicas)
│   ├── Calculation Service (4 replicas)
│   ├── Strategy Service (2 replicas)
│   └── Trading Service (2 replicas)
├── PostgreSQL (主从)
├── TimescaleDB
├── Redis Cluster
└── Kafka Cluster
```

---

## 技术选型总结

| 层次 | 技术 | 理由 |
|------|------|------|
| 前端框架 | Vue 3 | 轻量、响应式、生态完善 |
| 图表库 | ECharts | 功能强大、性能好 |
| 后端框架 | Spring Boot 4.0.1 | 成熟稳定、生态丰富 |
| 编程语言 | Java 21 | 最新 LTS 版本、性能提升 |
| 主数据库 | PostgreSQL | 功能强大、可靠性高 |
| 时序数据库 | TimescaleDB | 专为时序数据优化 |
| 缓存 | Redis | 高性能、功能丰富 |
| 消息队列 | Kafka | 高吞吐、持久化 |
| 容器化 | Docker | 标准化部署 |
| 编排 | Kubernetes | 自动化运维 |
| 监控 | Prometheus + Grafana | 开源、功能完善 |

---

## 下一步

查看 [数据模型设计](./06-data-models.md)
