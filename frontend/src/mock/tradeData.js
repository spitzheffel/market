// Mock 交易数据

// 生成持仓数据
export const mockPositions = [
  {
    id: 1,
    symbol: 'BTC/USDT',
    side: 'long',
    size: 0.5,
    entryPrice: 66800,
    currentPrice: 67420,
    pnl: 310,
    pnlPercent: 0.93,
    leverage: 1,
    margin: 33400,
    liquidationPrice: null,
    openTime: Date.now() - 3600000 * 12
  },
  {
    id: 2,
    symbol: 'ETH/USDT',
    side: 'long',
    size: 5,
    entryPrice: 3480,
    currentPrice: 3520,
    pnl: 200,
    pnlPercent: 1.15,
    leverage: 1,
    margin: 17400,
    liquidationPrice: null,
    openTime: Date.now() - 3600000 * 8
  },
  {
    id: 3,
    symbol: 'SOL/USDT',
    side: 'short',
    size: 50,
    entryPrice: 145,
    currentPrice: 142,
    pnl: 150,
    pnlPercent: 2.07,
    leverage: 1,
    margin: 7250,
    liquidationPrice: null,
    openTime: Date.now() - 3600000 * 4
  }
];

// 生成委托数据
export const mockOrders = [
  {
    id: 1001,
    symbol: 'BTC/USDT',
    side: 'buy',
    type: 'limit',
    price: 66500,
    size: 0.3,
    filled: 0,
    status: 'open',
    createTime: Date.now() - 3600000 * 2
  },
  {
    id: 1002,
    symbol: 'ETH/USDT',
    side: 'sell',
    type: 'limit',
    price: 3600,
    size: 3,
    filled: 0,
    status: 'open',
    createTime: Date.now() - 3600000 * 1
  },
  {
    id: 1003,
    symbol: 'BNB/USDT',
    side: 'buy',
    type: 'limit',
    price: 610,
    size: 10,
    filled: 0,
    status: 'open',
    createTime: Date.now() - 3600000 * 0.5
  },
  {
    id: 1004,
    symbol: 'SOL/USDT',
    side: 'buy',
    type: 'stop',
    price: 140,
    stopPrice: 141,
    size: 30,
    filled: 0,
    status: 'pending',
    createTime: Date.now() - 3600000 * 3
  }
];

// 生成成交数据
export const mockTrades = [
  {
    id: 2001,
    symbol: 'BTC/USDT',
    side: 'buy',
    price: 66800,
    size: 0.5,
    fee: 33.4,
    feeAsset: 'USDT',
    time: Date.now() - 3600000 * 12
  },
  {
    id: 2002,
    symbol: 'ETH/USDT',
    side: 'buy',
    price: 3480,
    size: 5,
    fee: 17.4,
    feeAsset: 'USDT',
    time: Date.now() - 3600000 * 8
  },
  {
    id: 2003,
    symbol: 'SOL/USDT',
    side: 'sell',
    price: 145,
    size: 50,
    fee: 7.25,
    feeAsset: 'USDT',
    time: Date.now() - 3600000 * 4
  },
  {
    id: 2004,
    symbol: 'BNB/USDT',
    side: 'sell',
    price: 615,
    size: 8,
    fee: 4.92,
    feeAsset: 'USDT',
    time: Date.now() - 3600000 * 24
  },
  {
    id: 2005,
    symbol: 'BTC/USDT',
    side: 'sell',
    price: 67200,
    size: 0.3,
    fee: 20.16,
    feeAsset: 'USDT',
    time: Date.now() - 3600000 * 36
  }
];

// 生成资金账户数据
export const mockAccount = {
  totalEquity: 125680.50,
  availableBalance: 67430.50,
  usedMargin: 58050.00,
  unrealizedPnl: 660.00,
  marginLevel: 216.5,
  positions: 3,
  openOrders: 4,
  todayPnl: 420.50,
  todayPnlPercent: 0.34,
  totalPnl: 8680.50,
  totalPnlPercent: 7.42
};

// 资金历史数据（用于图表）
export const mockEquityHistory = [
  { time: Date.now() - 3600000 * 24 * 7, equity: 117000 },
  { time: Date.now() - 3600000 * 24 * 6, equity: 118500 },
  { time: Date.now() - 3600000 * 24 * 5, equity: 119200 },
  { time: Date.now() - 3600000 * 24 * 4, equity: 121000 },
  { time: Date.now() - 3600000 * 24 * 3, equity: 122800 },
  { time: Date.now() - 3600000 * 24 * 2, equity: 123500 },
  { time: Date.now() - 3600000 * 24 * 1, equity: 125260 },
  { time: Date.now(), equity: 125680.50 }
];
