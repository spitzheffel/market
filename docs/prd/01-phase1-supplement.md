# Phase 1 补充：基础缠论计算 + 前端集成

## 1.4 数据完整性检测和修复（续）

### 技术实现

**缺失数据检测器：**

```typescript
class DataIntegrityChecker {
  async detectMissingKlines(
    symbol: string,
    interval: string,
    startTime: number,
    endTime: number
  ): Promise<number[]> {
    const intervalMs = this.getIntervalMs(interval);

    // 从数据库查询实际的K线
    const actualKlines = await this.db.query(
      'SELECT timestamp FROM klines ' +
      'WHERE symbol = ? AND interval = ? ' +
      'AND timestamp >= ? AND timestamp < ? ' +
      'ORDER BY timestamp',
      [symbol, interval, startTime, endTime]
    );

    // 找出缺失的时间点
    const missing: number[] = [];
    let expectedTime = startTime;
    let actualIndex = 0;

    while (expectedTime < endTime) {
      if (actualIndex >= actualKlines.length ||
          actualKlines[actualIndex].timestamp !== expectedTime) {
        missing.push(expectedTime);
      } else {
        actualIndex++;
      }
      expectedTime += intervalMs;
    }

    return missing;
  }

  // 将连续的缺失时间点合并为区间
  mergeToRanges(
    missingTimestamps: number[],
    intervalMs: number
  ): Array<{start: number, end: number}> {
    if (missingTimestamps.length === 0) return [];

    const ranges = [];
    let rangeStart = missingTimestamps[0];
    let rangeEnd = rangeStart + intervalMs;

    for (let i = 1; i < missingTimestamps.length; i++) {
      const current = missingTimestamps[i];

      if (current === rangeEnd) {
        rangeEnd = current + intervalMs;
      } else {
        ranges.push({ start: rangeStart, end: rangeEnd });
        rangeStart = current;
        rangeEnd = current + intervalMs;
      }
    }

    ranges.push({ start: rangeStart, end: rangeEnd });
    return ranges;
  }
}
```

**自动修复服务：**

```typescript
class AutoRepairService {
  private checkInterval = 3600000; // 每小时检查一次

  start() {
    console.log('[AutoRepair] Service started');
    setInterval(() => this.checkAndRepair(), this.checkInterval);
  }

  async checkAndRepair() {
    const symbols = await this.getMonitoredSymbols();
    const intervals = ['1m', '5m', '15m', '1h', '4h', '1d'];

    for (const symbol of symbols) {
      for (const interval of intervals) {
        // 检查最近7天的数据
        const endTime = Date.now();
        const startTime = endTime - 7 * 24 * 3600 * 1000;

        const missing = await this.checker.detectMissingKlines(
          symbol,
          interval,
          startTime,
          endTime
        );

        if (missing.length > 0) {
          console.log(
            `[AutoRepair] Found ${missing.length} missing klines ` +
            `for ${symbol} ${interval}`
          );

          // 合并为区间并回补
          const ranges = this.checker.mergeToRanges(
            missing,
            this.getIntervalMs(interval)
          );

          for (const range of ranges) {
            await this.backfill.backfill(
              symbol,
              interval,
              range.start,
              range.end
            );
          }

          // 回补完成后，触发重新计算
          this.emit('data-repaired', {
            symbol,
            interval,
            repairedTimestamps: missing
          });
        }
      }
    }
  }
}
```

---

## 1.5 基础缠论计算

### 功能需求

**FR-1.5.1 K线包含关系处理**
- 识别包含关系的K线
- 根据方向合并K线
- 生成处理后的K线序列

**FR-1.5.2 分型识别**
- 识别顶分型（三根K线，中间最高）
- 识别底分型（三根K线，中间最低）
- 分型确认机制

**FR-1.5.3 笔的构建**
- 连接相邻的顶底分型
- 验证笔的有效性（至少4根K线间隔）
- 笔的方向判断

### 技术实现

**数据模型：**

```typescript
// 处理包含关系后的K线
interface MergedKline {
  index: number;           // 在原始K线序列中的索引
  direction: 'up' | 'down' | null;  // 处理方向
  high: string;
  low: string;
  open: string;
  close: string;
  timestamp: number;
  originalKlines: number[];  // 包含的原始K线索引
}

// 分型
interface Fenxing {
  centerIndex: number;     // 中间K线索引
  leftIndex: number;       // 左侧K线索引
  rightIndex: number;      // 右侧K线索引
  type: 'top' | 'bottom';
  price: string;           // 分型价格
  timestamp: number;
  confirmed: boolean;      // 是否被后续K线确认
}

// 笔
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

**包含关系处理：**

```typescript
class KlineProcessor {
  // 处理包含关系
  processMergedKlines(klines: Kline[]): MergedKline[] {
    if (klines.length < 2) return [];

    const merged: MergedKline[] = [];
    let direction: 'up' | 'down' | null = null;

    // 第一根K线
    merged.push({
      index: 0,
      direction: null,
      high: klines[0].high,
      low: klines[0].low,
      open: klines[0].open,
      close: klines[0].close,
      timestamp: klines[0].timestamp,
      originalKlines: [0]
    });

    for (let i = 1; i < klines.length; i++) {
      const current = klines[i];
      const last = merged[merged.length - 1];

      // 判断是否包含
      const isContained = this.checkContainRelation(
        last.high,
        last.low,
        current.high,
        current.low
      );

      if (isContained) {
        // 有包含关系，需要合并
        if (direction === null) {
          // 还没有确定方向，根据前两根K线判断
          direction = this.getDirection(
            merged.length >= 2 ? merged[merged.length - 2] : null,
            last
          );
        }

        // 根据方向合并
        if (direction === 'up') {
          last.high = this.max(last.high, current.high);
          last.low = this.max(last.low, current.low);
        } else {
          last.high = this.min(last.high, current.high);
          last.low = this.min(last.low, current.low);
        }

        last.close = current.close;
        last.originalKlines.push(i);

      } else {
        // 没有包含关系，添加新K线
        merged.push({
          index: i,
          direction,
          high: current.high,
          low: current.low,
          open: current.open,
          close: current.close,
          timestamp: current.timestamp,
          originalKlines: [i]
        });

        // 更新方向
        direction = this.getDirection(last, merged[merged.length - 1]);
      }
    }

    return merged;
  }

  private checkContainRelation(
    high1: string,
    low1: string,
    high2: string,
    low2: string
  ): boolean {
    const h1 = parseFloat(high1);
    const l1 = parseFloat(low1);
    const h2 = parseFloat(high2);
    const l2 = parseFloat(low2);

    // K1包含K2 或 K2包含K1
    return (h1 >= h2 && l1 <= l2) || (h2 >= h1 && l2 <= l1);
  }

  private getDirection(
    prev: MergedKline | null,
    current: MergedKline
  ): 'up' | 'down' {
    if (!prev) return 'up';

    const prevHigh = parseFloat(prev.high);
    const currentHigh = parseFloat(current.high);

    return currentHigh > prevHigh ? 'up' : 'down';
  }
}
```

**分型识别：**

```typescript
class FenxingIdentifier {
  identifyFenxing(mergedKlines: MergedKline[]): Fenxing[] {
    const fenxing: Fenxing[] = [];

    for (let i = 1; i < mergedKlines.length - 1; i++) {
      const left = mergedKlines[i - 1];
      const center = mergedKlines[i];
      const right = mergedKlines[i + 1];

      // 检查顶分型
      if (this.isTopFenxing(left, center, right)) {
        fenxing.push({
          centerIndex: i,
          leftIndex: i - 1,
          rightIndex: i + 1,
          type: 'top',
          price: center.high,
          timestamp: center.timestamp,
          confirmed: false
        });
      }

      // 检查底分型
      if (this.isBottomFenxing(left, center, right)) {
        fenxing.push({
          centerIndex: i,
          leftIndex: i - 1,
          rightIndex: i + 1,
          type: 'bottom',
          price: center.low,
          timestamp: center.timestamp,
          confirmed: false
        });
      }
    }

    return fenxing;
  }

  private isTopFenxing(
    left: MergedKline,
    center: MergedKline,
    right: MergedKline
  ): boolean {
    const leftHigh = parseFloat(left.high);
    const centerHigh = parseFloat(center.high);
    const rightHigh = parseFloat(right.high);

    return centerHigh > leftHigh && centerHigh > rightHigh;
  }

  private isBottomFenxing(
    left: MergedKline,
    center: MergedKline,
    right: MergedKline
  ): boolean {
    const leftLow = parseFloat(left.low);
    const centerLow = parseFloat(center.low);
    const rightLow = parseFloat(right.low);

    return centerLow < leftLow && centerLow < rightLow;
  }
}
```

**笔的构建：**

```typescript
class BiBuilder {
  buildBi(fenxing: Fenxing[]): Bi[] {
    const bi: Bi[] = [];

    for (let i = 0; i < fenxing.length - 1; i++) {
      const start = fenxing[i];
      const end = fenxing[i + 1];

      // 只有顶底交替才形成笔
      if (start.type === end.type) {
        continue;
      }

      // 检查K线数量（至少4根）
      const klineCount = end.centerIndex - start.centerIndex;
      if (klineCount < 4) {
        continue;
      }

      bi.push({
        id: `bi_${start.centerIndex}_${end.centerIndex}`,
        startFenxing: start,
        endFenxing: end,
        direction: start.type === 'bottom' ? 'up' : 'down',
        klineCount,
        startPrice: start.price,
        endPrice: end.price,
        startTime: start.timestamp,
        endTime: end.timestamp,
        confirmed: false
      });
    }

    return bi;
  }
}
```

**缠论计算引擎：**

```typescript
class ChanTheoryEngine {
  calculate(klines: Kline[]): ChanResult {
    // 1. 处理包含关系
    const mergedKlines = this.klineProcessor.processMergedKlines(klines);

    // 2. 识别分型
    const fenxing = this.fenxingIdentifier.identifyFenxing(mergedKlines);

    // 3. 构建笔
    const bi = this.biBuilder.buildBi(fenxing);

    return {
      klines,
      mergedKlines,
      fenxing,
      bi,
      calculatedAt: Date.now()
    };
  }
}
```

---

## 1.6 前端集成

### 功能需求

**FR-1.6.1 实时图表展示**
- 显示 K线图
- 显示分型标记
- 显示笔的连线
- 实时更新

**FR-1.6.2 交互功能**
- 缩放和拖拽
- 十字线跟随
- 数据提示框
- 周期切换

**FR-1.6.3 WebSocket 连接**
- 连接后端 WebSocket
- 订阅指定标的和周期
- 接收实时更新
- 断线重连

### 技术实现

**前端 WebSocket 客户端：**

```typescript
class ChanWebSocketClient {
  private ws: WebSocket;
  private reconnectAttempts = 0;

  connect() {
    this.ws = new WebSocket('ws://localhost:8080/ws/chan');

    this.ws.onopen = () => {
      console.log('[WS] Connected');
      this.reconnectAttempts = 0;
      this.subscribe();
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };

    this.ws.onclose = () => {
      console.log('[WS] Closed');
      this.reconnect();
    };
  }

  subscribe() {
    this.send({
      type: 'subscribe',
      symbol: 'BTC/USDT',
      interval: '1m'
    });
  }

  handleMessage(message: any) {
    switch (message.type) {
      case 'kline-update':
        this.updateChart(message.data);
        break;
      case 'kline-closed':
        this.addNewKline(message.data);
        break;
      case 'chan-result':
        this.updateChanStructure(message.data);
        break;
    }
  }

  reconnect() {
    if (this.reconnectAttempts >= 10) {
      console.error('[WS] Max reconnect attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

    setTimeout(() => this.connect(), delay);
  }
}
```

**图表组件更新：**

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ChanWebSocketClient } from '@/services/ChanWebSocketClient';

const chartData = ref<ChanResult | null>(null);
const wsClient = new ChanWebSocketClient();

onMounted(() => {
  wsClient.connect();
  wsClient.on('chan-result', (data) => {
    chartData.value = data;
  });
});

onUnmounted(() => {
  wsClient.disconnect();
});
</script>
```

---

## Phase 1 总结

### 完成标准

**数据层：**
- [x] 可以连接多个交易所
- [x] 可以获取历史数据
- [x] 可以接收实时数据
- [x] 数据完整性有保障
- [x] 延迟监控正常

**计算层：**
- [x] 可以正确处理包含关系
- [x] 可以识别分型
- [x] 可以构建笔
- [x] 计算结果准确

**前端：**
- [x] 可以实时显示K线图
- [x] 可以显示分型和笔
- [x] 交互流畅

### 性能指标

- 数据延迟 < 100ms
- 计算延迟 < 50ms
- 前端渲染 < 16ms (60fps)
- 数据库查询 < 100ms

### 下一步

进入 [Phase 2: 完善缠论计算](./02-phase2-chan-calculation.md)
