// Mock K线数据和缠论标注数据

// 生成K线数据 (OHLCV)
function generateKlineData(basePrice = 67000, count = 120) {
  const data = [];
  const startTime = Date.now() - count * 60000; // 1分钟K线
  let price = basePrice;

  for (let i = 0; i < count; i++) {
    const timestamp = startTime + i * 60000;
    const volatility = 0.002; // 0.2% 波动
    const trend = Math.sin(i / 10) * 0.001; // 添加趋势

    const change = (Math.random() - 0.5) * volatility + trend;
    const open = price;
    const close = price * (1 + change);
    const high = Math.max(open, close) * (1 + Math.random() * 0.001);
    const low = Math.min(open, close) * (1 - Math.random() * 0.001);
    const volume = Math.random() * 100 + 50;

    data.push({
      timestamp,
      date: new Date(timestamp).toLocaleString(undefined, {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }),
      open: open.toFixed(2),
      high: high.toFixed(2),
      low: low.toFixed(2),
      close: close.toFixed(2),
      volume: volume.toFixed(2)
    });

    price = close;
  }

  return data;
}

// 生成分型数据（顶分型和底分型）
function generateFenxingData(klineData) {
  const fenxing = [];

  // 在K线数据中找一些局部高低点作为分型
  for (let i = 5; i < klineData.length - 5; i += 8) {
    const isTop = Math.random() > 0.5;
    const index = i + Math.floor(Math.random() * 3);

    if (index < klineData.length) {
      fenxing.push({
        index,
        type: isTop ? 'top' : 'bottom',
        price: isTop ? klineData[index].high : klineData[index].low,
        timestamp: klineData[index].timestamp
      });
    }
  }

  return fenxing.sort((a, b) => a.index - b.index);
}

// 生成笔数据（连接相邻的顶底分型）
function generateBiData(fenxingData) {
  const bi = [];

  for (let i = 0; i < fenxingData.length - 1; i++) {
    const start = fenxingData[i];
    const end = fenxingData[i + 1];

    // 只有顶底交替才形成笔
    if (start.type !== end.type) {
      bi.push({
        startIndex: start.index,
        endIndex: end.index,
        startPrice: start.price,
        endPrice: end.price,
        direction: start.type === 'bottom' ? 'up' : 'down'
      });
    }
  }

  return bi;
}

// 生成线段数据（更高级别的结构）
function generateXianduanData(biData) {
  const xianduan = [];

  // 每3-5笔形成一个线段
  for (let i = 0; i < biData.length - 3; i += 4) {
    const start = biData[i];
    const end = biData[Math.min(i + 3, biData.length - 1)];

    xianduan.push({
      startIndex: start.startIndex,
      endIndex: end.endIndex,
      startPrice: start.startPrice,
      endPrice: end.endPrice,
      direction: parseFloat(end.endPrice) > parseFloat(start.startPrice) ? 'up' : 'down'
    });
  }

  return xianduan;
}

// 生成中枢数据
function generateZhongshuData(klineData, xianduanData) {
  const zhongshu = [];

  // 在线段重叠区域形成中枢
  for (let i = 0; i < xianduanData.length - 2; i += 3) {
    const x1 = xianduanData[i];
    const x2 = xianduanData[i + 1];
    const x3 = xianduanData[i + 2];

    const prices = [
      parseFloat(x1.startPrice),
      parseFloat(x1.endPrice),
      parseFloat(x2.startPrice),
      parseFloat(x2.endPrice),
      parseFloat(x3.startPrice),
      parseFloat(x3.endPrice)
    ];

    const high = Math.max(...prices);
    const low = Math.min(...prices);
    const zhongshuHigh = low + (high - low) * 0.7;
    const zhongshuLow = low + (high - low) * 0.3;

    zhongshu.push({
      startIndex: x1.startIndex,
      endIndex: x3.endIndex,
      high: zhongshuHigh.toFixed(2),
      low: zhongshuLow.toFixed(2),
      center: ((zhongshuHigh + zhongshuLow) / 2).toFixed(2)
    });
  }

  return zhongshu;
}

// 生成买卖点数据
function generateTradingPoints(fenxingData, biData) {
  const points = [];

  // 在一些底分型位置标记买点，顶分型位置标记卖点
  fenxingData.forEach((fx, i) => {
    // 不是每个分型都是买卖点，随机选择一些
    if (Math.random() > 0.6) {
      const isBuyPoint = fx.type === 'bottom';

      // 根据位置判断是第几类买卖点
      let level = 1;
      if (i > fenxingData.length * 0.3 && i < fenxingData.length * 0.7) {
        level = 2;
      } else if (i > fenxingData.length * 0.7) {
        level = 3;
      }

      points.push({
        index: fx.index,
        type: isBuyPoint ? 'buy' : 'sell',
        level, // 1买/1卖, 2买/2卖, 3买/3卖
        price: fx.price,
        timestamp: fx.timestamp
      });
    }
  });

  return points.sort((a, b) => a.index - b.index);
}

// 生成完整的图表数据
export function generateChartData(symbol = 'BTC/USDT', basePrice = 67000) {
  const klineData = generateKlineData(basePrice, 120);
  const fenxingData = generateFenxingData(klineData);
  const biData = generateBiData(fenxingData);
  const xianduanData = generateXianduanData(biData);
  const zhongshuData = generateZhongshuData(klineData, xianduanData);
  const tradingPoints = generateTradingPoints(fenxingData, biData);

  return {
    symbol,
    interval: '1m',
    klineData,
    chanTheory: {
      fenxing: fenxingData,
      bi: biData,
      xianduan: xianduanData,
      zhongshu: zhongshuData,
      tradingPoints
    }
  };
}

// 预生成几个常用标的的数据
export const mockChartData = {
  'BTC/USDT': generateChartData('BTC/USDT', 67000),
  'ETH/USDT': generateChartData('ETH/USDT', 3500),
  'SOL/USDT': generateChartData('SOL/USDT', 142),
  'BNB/USDT': generateChartData('BNB/USDT', 612)
};

// 获取指定标的的图表数据
export function getChartData(symbol) {
  return mockChartData[symbol] || mockChartData['BTC/USDT'];
}
