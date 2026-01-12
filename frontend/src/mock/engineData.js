// Mock 缠论引擎计算结果数据

// 当前笔的数据
export const mockCurrentBi = {
  direction: 'up',
  startIndex: 95,
  endIndex: 118,
  startPrice: 66800,
  endPrice: 67420,
  startTime: Date.now() - 3600000 * 2,
  endTime: Date.now() - 3600000 * 0.5,
  klineCount: 23,
  strength: 'strong' // strong, medium, weak
};

// 当前线段的数据
export const mockCurrentXianduan = {
  direction: 'up',
  startIndex: 72,
  endIndex: 118,
  startPrice: 66200,
  endPrice: 67420,
  startTime: Date.now() - 3600000 * 5,
  endTime: Date.now() - 3600000 * 0.5,
  biCount: 5,
  strength: 'strong'
};

// 当前中枢的数据
export const mockCurrentZhongshu = {
  level: '1m', // 级别
  startIndex: 45,
  endIndex: 95,
  high: 67100,
  low: 66500,
  center: 66800,
  startTime: Date.now() - 3600000 * 8,
  endTime: Date.now() - 3600000 * 2,
  xianduanCount: 3,
  oscillations: 5, // 震荡次数
  type: 'expanding' // expanding, contracting, stable
};

// 最近的分型列表
export const mockRecentFenxing = [
  {
    index: 118,
    type: 'top',
    price: 67420,
    time: Date.now() - 3600000 * 0.5,
    confirmed: true
  },
  {
    index: 105,
    type: 'bottom',
    price: 66950,
    time: Date.now() - 3600000 * 1.5,
    confirmed: true
  },
  {
    index: 95,
    type: 'top',
    price: 67200,
    time: Date.now() - 3600000 * 2,
    confirmed: true
  },
  {
    index: 82,
    type: 'bottom',
    price: 66800,
    time: Date.now() - 3600000 * 3,
    confirmed: true
  },
  {
    index: 72,
    type: 'top',
    price: 67050,
    time: Date.now() - 3600000 * 4,
    confirmed: true
  }
];

// 最近的买卖点
export const mockRecentTradingPoints = [
  {
    index: 118,
    type: 'sell',
    level: 1,
    price: 67420,
    time: Date.now() - 3600000 * 0.5,
    confidence: 'high', // high, medium, low
    reason: '一卖：笔背驰'
  },
  {
    index: 95,
    type: 'buy',
    level: 2,
    price: 66800,
    time: Date.now() - 3600000 * 2,
    confidence: 'medium',
    reason: '二买：中枢回抽'
  },
  {
    index: 72,
    type: 'buy',
    level: 1,
    price: 66200,
    time: Date.now() - 3600000 * 5,
    confidence: 'high',
    reason: '一买：线段背驰'
  }
];

// 引擎统计数据
export const mockEngineStats = {
  totalFenxing: 45,
  topFenxing: 23,
  bottomFenxing: 22,
  totalBi: 22,
  upBi: 11,
  downBi: 11,
  totalXianduan: 8,
  upXianduan: 4,
  downXianduan: 4,
  totalZhongshu: 3,
  totalTradingPoints: 12,
  buyPoints: 6,
  sellPoints: 6,
  lastUpdateTime: Date.now()
};

// 引擎性能数据
export const mockEnginePerformance = {
  avgCalculationTime: 12.5, // ms
  lastCalculationTime: 11.8, // ms
  calculationsPerSecond: 80,
  memoryUsage: 45.2, // MB
  cpuUsage: 8.5 // %
};
