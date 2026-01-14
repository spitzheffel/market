# Phase 2: 完善缠论计算

## 阶段概述

**时间**: 3-4 周
**目标**: 实现完整准确的缠论算法
**交付物**: 可以准确识别线段、中枢和买卖点的系统

---

## 阶段目标

### 核心目标
1. ✅ 实现线段识别算法
2. ✅ 实现中枢识别算法
3. ✅ 实现买卖点判定（含背驰分析）
4. ✅ 建立完整的测试体系
5. ✅ 优化计算性能

### 为什么这个阶段很关键？

**缠论算法是系统的核心：**
- 算法错误会导致所有信号都不可靠
- 必须严格按照缠论定义实现
- 需要大量测试验证正确性

**性能优化至关重要：**
- 实时计算要求 < 10ms
- 支持多标的并行计算
- 增量计算避免重复计算

---

## Week 5-6: 线段识别 + 中枢识别

### 2.1 线段识别

#### 功能需求

**FR-2.1.1 特征序列构建**
- 从笔的端点构建特征序列
- 区分顶分型和底分型的特征序列
- 特征序列的方向判断

**FR-2.1.2 线段划分**
- 识别线段的起点和终点
- 判断线段是否被破坏
- 线段的方向和强度

**FR-2.1.3 线段确认**
- 线段的确认条件
- 未确认线段的处理
- 线段的更新机制

#### 技术实现

**数据模型：**

```typescript
// 特征序列元素
interface TezhengElement {
  index: number;           // 在笔序列中的索引
  bi: Bi;                  // 对应的笔
  price: string;           // 特征价格
  timestamp: number;
  type: 'top' | 'bottom';
}

// 线段
interface Xianduan {
  id: string;
  startBi: Bi;             // 起始笔
  endBi: Bi;               // 结束笔
  biList: Bi[];            // 包含的所有笔
  direction: 'up' | 'down';
  startPrice: string;
  endPrice: string;
  startTime: number;
  endTime: number;
  confirmed: boolean;
  broken: boolean;         // 是否被破坏
  tezhengSequence: TezhengElement[];  // 特征序列
}
```

**特征序列构建：**

```typescript
class TezhengSequenceBuilder {
  buildSequence(biList: Bi[]): TezhengElement[] {
    const sequence: TezhengElement[] = [];

    for (let i = 0; i < biList.length; i++) {
      const bi = biList[i];

      // 笔的终点作为特征序列元素
      sequence.push({
        index: i,
        bi,
        price: bi.endPrice,
        timestamp: bi.endTime,
        type: bi.direction === 'up' ? 'top' : 'bottom'
      });
    }

    return sequence;
  }

  // 检查特征序列是否形成线段破坏
  checkBreakdown(
    sequence: TezhengElement[],
    direction: 'up' | 'down'
  ): boolean {
    if (sequence.length < 3) return false;

    // 线段破坏的条件：
    // 上升线段：后面的底分型低于前面的底分型
    // 下降线段：后面的顶分型高于前面的顶分型

    if (direction === 'up') {
      // 找到所有底分型
      const bottomElements = sequence.filter(e => e.type === 'bottom');
      if (bottomElements.length < 2) return false;

      // 检查是否有后面的底分型低于前面的
      for (let i = 1; i < bottomElements.length; i++) {
        const prev = parseFloat(bottomElements[i - 1].price);
        const current = parseFloat(bottomElements[i].price);
        if (current < prev) {
          return true;
        }
      }
    } else {
      // 找到所有顶分型
      const topElements = sequence.filter(e => e.type === 'top');
      if (topElements.length < 2) return false;

      // 检查是否有后面的顶分型高于前面的
      for (let i = 1; i < topElements.length; i++) {
        const prev = parseFloat(topElements[i - 1].price);
        const current = parseFloat(topElements[i].price);
        if (current > prev) {
          return true;
        }
      }
    }

    return false;
  }
}
```

**线段识别器：**

```typescript
class XianduanIdentifier {
  identifyXianduan(biList: Bi[]): Xianduan[] {
    const xianduan: Xianduan[] = [];
    let currentXianduan: Partial<Xianduan> | null = null;

    for (let i = 0; i < biList.length; i++) {
      const bi = biList[i];

      if (!currentXianduan) {
        // 开始新线段
        currentXianduan = {
          startBi: bi,
          biList: [bi],
          direction: bi.direction,
          startPrice: bi.startPrice,
          startTime: bi.startTime,
          confirmed: false,
          broken: false
        };
        continue;
      }

      // 添加笔到当前线段
      currentXianduan.biList!.push(bi);

      // 构建特征序列
      const tezhengSequence = this.tezhengBuilder.buildSequence(
        currentXianduan.biList!
      );

      // 检查是否破坏
      const broken = this.tezhengBuilder.checkBreakdown(
        tezhengSequence,
        currentXianduan.direction!
      );

      if (broken) {
        // 线段被破坏，完成当前线段
        currentXianduan.endBi = currentXianduan.biList![
          currentXianduan.biList!.length - 2
        ];
        currentXianduan.endPrice = currentXianduan.endBi.endPrice;
        currentXianduan.endTime = currentXianduan.endBi.endTime;
        currentXianduan.broken = true;
        currentXianduan.confirmed = true;
        currentXianduan.tezhengSequence = tezhengSequence;
        currentXianduan.id = `xd_${currentXianduan.startBi.id}_${currentXianduan.endBi.id}`;

        xianduan.push(currentXianduan as Xianduan);

        // 开始新线段
        currentXianduan = {
          startBi: bi,
          biList: [bi],
          direction: bi.direction,
          startPrice: bi.startPrice,
          startTime: bi.startTime,
          confirmed: false,
          broken: false
        };
      }
    }

    // 处理未完成的线段
    if (currentXianduan && currentXianduan.biList!.length >= 3) {
      currentXianduan.endBi = currentXianduan.biList![
        currentXianduan.biList!.length - 1
      ];
      currentXianduan.endPrice = currentXianduan.endBi!.endPrice;
      currentXianduan.endTime = currentXianduan.endBi!.endTime;
      currentXianduan.confirmed = false;
      currentXianduan.id = `xd_${currentXianduan.startBi!.id}_${currentXianduan.endBi!.id}`;

      xianduan.push(currentXianduan as Xianduan);
    }

    return xianduan;
  }
}
```

---

### 2.2 中枢识别

#### 功能需求

**FR-2.2.1 中枢定义**
- 至少3笔/线段的重叠区间
- 中枢的上轨、下轨、中轨
- 中枢的级别（笔中枢、线段中枢）

**FR-2.2.2 中枢类型**
- 中枢的扩展（震荡）
- 中枢的新生
- 中枢的移动

**FR-2.2.3 中枢位置判断**
- 当前价格在中枢内/上/下
- 突破中枢的判断
- 回抽中枢的判断

#### 技术实现

**数据模型：**

```typescript
// 中枢
interface Zhongshu {
  id: string;
  level: 'bi' | 'xianduan';  // 级别
  components: Bi[] | Xianduan[];  // 构成中枢的笔或线段
  high: string;              // 上轨
  low: string;               // 下轨
  center: string;            // 中轨
  startTime: number;
  endTime: number;
  type: 'extending' | 'new' | 'moving';  // 类型
  oscillations: number;      // 震荡次数
  confirmed: boolean;
}
```

**中枢识别器：**

```typescript
class ZhongshuIdentifier {
  // 识别笔中枢
  identifyBiZhongshu(biList: Bi[]): Zhongshu[] {
    const zhongshu: Zhongshu[] = [];
    let i = 0;

    while (i < biList.length - 2) {
      // 取3笔检查是否有重叠
      const bi1 = biList[i];
      const bi2 = biList[i + 1];
      const bi3 = biList[i + 2];

      const overlap = this.calculateOverlap([bi1, bi2, bi3]);

      if (overlap) {
        // 找到中枢，继续向后扩展
        const components = [bi1, bi2, bi3];
        let j = i + 3;

        while (j < biList.length) {
          const nextBi = biList[j];
          const newOverlap = this.calculateOverlap([...components, nextBi]);

          if (newOverlap) {
            // 仍然有重叠，继续扩展
            components.push(nextBi);
            j++;
          } else {
            // 没有重叠，中枢结束
            break;
          }
        }

        // 创建中枢
        zhongshu.push({
          id: `zs_bi_${i}`,
          level: 'bi',
          components,
          high: overlap.high,
          low: overlap.low,
          center: this.calculateCenter(overlap.high, overlap.low),
          startTime: components[0].startTime,
          endTime: components[components.length - 1].endTime,
          type: 'extending',
          oscillations: Math.floor(components.length / 2),
          confirmed: true
        });

        i = j;
      } else {
        i++;
      }
    }

    return zhongshu;
  }

  // 计算重叠区间
  private calculateOverlap(
    components: Bi[] | Xianduan[]
  ): { high: string; low: string } | null {
    if (components.length < 3) return null;

    // 找出所有的高点和低点
    const highs: number[] = [];
    const lows: number[] = [];

    for (const comp of components) {
      if ('direction' in comp) {
        // Bi
        const startPrice = parseFloat(comp.startPrice);
        const endPrice = parseFloat(comp.endPrice);
        highs.push(Math.max(startPrice, endPrice));
        lows.push(Math.min(startPrice, endPrice));
      }
    }

    // 重叠区间 = [max(所有低点), min(所有高点)]
    const overlapLow = Math.max(...lows);
    const overlapHigh = Math.min(...highs);

    // 如果 low > high，说明没有重叠
    if (overlapLow >= overlapHigh) {
      return null;
    }

    return {
      high: overlapHigh.toString(),
      low: overlapLow.toString()
    };
  }

  private calculateCenter(high: string, low: string): string {
    const h = parseFloat(high);
    const l = parseFloat(low);
    return ((h + l) / 2).toString();
  }

  // 判断价格相对于中枢的位置
  getPricePosition(
    price: string,
    zhongshu: Zhongshu
  ): 'inside' | 'above' | 'below' {
    const p = parseFloat(price);
    const high = parseFloat(zhongshu.high);
    const low = parseFloat(zhongshu.low);

    if (p > high) return 'above';
    if (p < low) return 'below';
    return 'inside';
  }
}
```

---

## Week 7-8: 买卖点判定 + 测试优化

### 2.3 买卖点判定

#### 功能需求

**FR-2.3.1 一类买卖点**
- 趋势转折点
- 需要背驰判断
- 最强的买卖点

**FR-2.3.2 二类买卖点**
- 回抽确认点
- 突破后的回测
- 次强的买卖点

**FR-2.3.3 三类买卖点**
- 中枢震荡点
- 中枢边缘的买卖机会
- 风险相对较高

**FR-2.3.4 背驰判断**
- MACD 背驰
- 成交量背驰
- 背驰强度评估

#### 技术实现

**数据模型：**

```typescript
// 买卖点
interface TradingPoint {
  id: string;
  type: 'buy' | 'sell';
  level: 1 | 2 | 3;        // 买卖点级别
  index: number;           // K线索引
  price: string;
  timestamp: number;
  reason: string;          // 判定理由
  confidence: 'high' | 'medium' | 'low';  // 置信度
  divergence?: {           // 背驰数据
    macd: number;
    volume: number;
    type: 'bullish' | 'bearish';
  };
  relatedStructures: {     // 相关结构
    bi?: Bi;
    xianduan?: Xianduan;
    zhongshu?: Zhongshu;
  };
}
```

**MACD 计算器：**

```typescript
class MACDCalculator {
  calculate(klines: Kline[]): MACDResult[] {
    const closes = klines.map(k => parseFloat(k.close));

    // 计算 EMA
    const ema12 = this.calculateEMA(closes, 12);
    const ema26 = this.calculateEMA(closes, 26);

    // 计算 DIF
    const dif = ema12.map((v, i) => v - ema26[i]);

    // 计算 DEA (DIF的9日EMA)
    const dea = this.calculateEMA(dif, 9);

    // 计算 MACD柱
    const macd = dif.map((v, i) => (v - dea[i]) * 2);

    return macd.map((m, i) => ({
      timestamp: klines[i].timestamp,
      dif: dif[i],
      dea: dea[i],
      macd: m
    }));
  }

  private calculateEMA(data: number[], period: number): number[] {
    const k = 2 / (period + 1);
    const ema: number[] = [];

    // 第一个值使用 SMA
    let sum = 0;
    for (let i = 0; i < period && i < data.length; i++) {
      sum += data[i];
    }
    ema[period - 1] = sum / period;

    // 后续值使用 EMA 公式
    for (let i = period; i < data.length; i++) {
      ema[i] = data[i] * k + ema[i - 1] * (1 - k);
    }

    return ema;
  }
}
```

**背驰判断器：**

```typescript
class DivergenceDetector {
  // 检测背驰
  detectDivergence(
    xianduan1: Xianduan,
    xianduan2: Xianduan,
    macdData: MACDResult[],
    volumeData: Kline[]
  ): DivergenceResult | null {
    // 价格比较
    const price1 = parseFloat(xianduan1.endPrice);
    const price2 = parseFloat(xianduan2.endPrice);

    // MACD 比较
    const macd1 = this.getMACDArea(xianduan1, macdData);
    const macd2 = this.getMACDArea(xianduan2, macdData);

    // 成交量比较
    const volume1 = this.getVolumeSum(xianduan1, volumeData);
    const volume2 = this.getVolumeSum(xianduan2, volumeData);

    // 判断背驰类型
    if (xianduan1.direction === 'up') {
      // 上涨线段，检查顶背驰
      if (price2 > price1 && macd2 < macd1) {
        return {
          type: 'bearish',
          priceChange: (price2 - price1) / price1,
          macdChange: (macd2 - macd1) / Math.abs(macd1),
          volumeChange: (volume2 - volume1) / volume1,
          strength: this.calculateDivergenceStrength(
            price2 - price1,
            macd2 - macd1
          )
        };
      }
    } else {
      // 下跌线段，检查底背驰
      if (price2 < price1 && macd2 > macd1) {
        return {
          type: 'bullish',
          priceChange: (price2 - price1) / price1,
          macdChange: (macd2 - macd1) / Math.abs(macd1),
          volumeChange: (volume2 - volume1) / volume1,
          strength: this.calculateDivergenceStrength(
            price1 - price2,
            macd1 - macd2
          )
        };
      }
    }

    return null;
  }

  private getMACDArea(
    xianduan: Xianduan,
    macdData: MACDResult[]
  ): number {
    // 计算线段对应时间段的 MACD 面积
    const startTime = xianduan.startTime;
    const endTime = xianduan.endTime;

    const relevantMACD = macdData.filter(
      m => m.timestamp >= startTime && m.timestamp <= endTime
    );

    return relevantMACD.reduce((sum, m) => sum + Math.abs(m.macd), 0);
  }

  private getVolumeSum(
    xianduan: Xianduan,
    volumeData: Kline[]
  ): number {
    const startTime = xianduan.startTime;
    const endTime = xianduan.endTime;

    const relevantVolume = volumeData.filter(
      k => k.timestamp >= startTime && k.timestamp <= endTime
    );

    return relevantVolume.reduce(
      (sum, k) => sum + parseFloat(k.volume),
      0
    );
  }

  private calculateDivergenceStrength(
    priceChange: number,
    macdChange: number
  ): 'strong' | 'medium' | 'weak' {
    const ratio = Math.abs(macdChange / priceChange);

    if (ratio > 2) return 'strong';
    if (ratio > 1) return 'medium';
    return 'weak';
  }
}
```

继续下一个文件...
