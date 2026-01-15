import axios from 'axios'
import type { AxiosInstance, AxiosError } from 'axios'

// API 基础配置
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`, config.params)
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  (error: AxiosError) => {
    console.error('[API Error]', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

// K线数据接口
export interface Kline {
  symbol: string
  interval: string
  time: string
  open: string
  high: string
  low: string
  close: string
  volume: string
}

// 分型接口
export interface Fenxing {
  id: string
  type: 'TOP' | 'BOTTOM'
  centerIndex: number
  price: string
  time: string
  confirmed: boolean
}

// 笔接口（后端返回时间戳，前端需转换为索引）
export interface Bi {
  id: string
  direction: 'UP' | 'DOWN'
  startFenxing?: { centerIndex: number }
  endFenxing?: { centerIndex: number }
  startPrice: string
  endPrice: string
  startTime: number
  endTime: number
  klineCount: number
}

// 缠论计算结果
export interface ChanResult {
  mergedKlines: any[]
  fenxings: Fenxing[]
  bis: Bi[]
}

// 线段接口
export interface Xianduan {
  id: string
  direction: 'UP' | 'DOWN'
  startPrice: string
  endPrice: string
  startTime: number
  endTime: number
  biCount: number
  confirmed: boolean
}

// 中枢接口
export interface Zhongshu {
  id: string
  level: 'BI' | 'XIANDUAN'
  high: string
  low: string
  center: string
  startTime: number
  endTime: number
  oscillations: number
}

// 买卖点接口
export interface TradingPoint {
  id: string
  type: 'BUY' | 'SELL'
  level: number
  price: string
  timestamp: number
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
  reason: string
}

// 完整缠论计算结果
export interface ChanResultFull {
  mergedKlines: any[]
  fenxings: Fenxing[]
  bis: Bi[]
  xianduans: Xianduan[]
  zhongshus: Zhongshu[]
  tradingPoints: TradingPoint[]
}

// 完整缠论分析响应（含K线和完整结果）
export interface ChanAnalysisResponseFull {
  klines: Kline[]
  result: ChanResultFull
}

// 健康状态
export interface HealthStatus {
  healthy: boolean
  exchange: string
  latency: number
  message?: string
  timestamp: number
}

// Ticker 接口
export interface Ticker {
  symbol: string
  lastPrice: string
  high: string
  low: string
  volume: string
  priceChange: string
  priceChangePercent: string
}

/**
 * K线 API
 */
export const klineApi = {
  /**
   * 获取 K线数据
   */
  async getKlines(params: {
    symbol: string
    interval: string
    startTime?: number
    endTime?: number
    limit?: number
    exchange?: string
  }): Promise<Kline[]> {
    const response = await apiClient.get('/api/klines', { params })
    return response.data
  },

  /**
   * 获取最新 K线
   */
  async getLatestKlines(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<Kline[]> {
    const response = await apiClient.get('/api/klines/latest', { params })
    return response.data
  },

  /**
   * 获取行情
   */
  async getTicker(symbol: string, exchange: string = 'binance'): Promise<Ticker> {
    const response = await apiClient.get('/api/klines/ticker', {
      params: { symbol, exchange }
    })
    return response.data
  },

  /**
   * 健康检查
   */
  async healthCheck(exchange: string = 'binance'): Promise<HealthStatus> {
    const response = await apiClient.get('/api/klines/health', {
      params: { exchange }
    })
    return response.data
  },

  /**
   * 获取可用交易所
   */
  async getExchanges(): Promise<{ available: string[]; defaultExchange: string }> {
    const response = await apiClient.get('/api/klines/exchanges')
    return response.data
  }
}

/**
 * 缠论计算 API
 */
export const chanApi = {
  /**
   * 完整缠论计算
   */
  async calculate(params: {
    symbol: string
    interval: string
    startTime?: number
    endTime?: number
    limit?: number
    exchange?: string
  }): Promise<ChanResult> {
    const response = await apiClient.get('/api/chan/calculate', { params })
    return response.data
  },

  /**
   * 完整分析（含K线和完整缠论结果）
   */
  async getAnalysisFull(params: {
    symbol: string
    interval: string
    startTime?: number
    endTime?: number
    limit?: number
    exchange?: string
  }): Promise<ChanAnalysisResponseFull> {
    const response = await apiClient.get('/api/chan/analysis', { params })
    return response.data
  },

  /**
   * 获取分型
   */
  async getFenxings(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<Fenxing[]> {
    const response = await apiClient.get('/api/chan/fenxings', { params })
    return response.data
  },

  /**
   * 获取笔
   */
  async getBis(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<Bi[]> {
    const response = await apiClient.get('/api/chan/bis', { params })
    return response.data
  },

  /**
   * 获取统计
   */
  async getStats(params: {
    symbol: string
    interval: string
    exchange?: string
  }): Promise<Record<string, any>> {
    const response = await apiClient.get('/api/chan/stats', { params })
    return response.data
  },

  /**
   * 完整缠论计算（包含线段、中枢、买卖点）
   */
  async calculateFull(params: {
    symbol: string
    interval: string
    startTime?: number
    endTime?: number
    limit?: number
    exchange?: string
  }): Promise<ChanResultFull> {
    const response = await apiClient.get('/api/chan/calculate-full', { params })
    return response.data
  },

  /**
   * 获取线段
   */
  async getXianduans(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<Xianduan[]> {
    const response = await apiClient.get('/api/chan/xianduans', { params })
    return response.data
  },

  /**
   * 获取中枢
   */
  async getZhongshus(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<Zhongshu[]> {
    const response = await apiClient.get('/api/chan/zhongshus', { params })
    return response.data
  },

  /**
   * 获取买卖点
   */
  async getTradingPoints(params: {
    symbol: string
    interval: string
    limit?: number
    exchange?: string
  }): Promise<TradingPoint[]> {
    const response = await apiClient.get('/api/chan/trading-points', { params })
    return response.data
  }
}

/**
 * 实时同步 API
 */
export const realtimeApi = {
  /**
   * 订阅
   */
  async subscribe(params: {
    symbol: string
    interval: string
    exchange?: string
    saveToDb?: boolean
  }): Promise<{ success: boolean }> {
    const response = await apiClient.post('/api/realtime/subscribe', null, { params })
    return response.data
  },

  /**
   * 取消订阅
   */
  async unsubscribe(params: {
    symbol: string
    interval: string
    exchange?: string
  }): Promise<{ success: boolean }> {
    const response = await apiClient.post('/api/realtime/unsubscribe', null, { params })
    return response.data
  },

  /**
   * 获取订阅列表
   */
  async getSubscriptions(): Promise<string[]> {
    const response = await apiClient.get('/api/realtime/subscriptions')
    return response.data
  }
}

/**
 * 数据完整性 API
 */
export const integrityApi = {
  /**
   * 查询缺口
   */
  async findGaps(params: {
    symbol: string
    interval: string
    startTime?: number
    endTime?: number
  }): Promise<any[]> {
    const response = await apiClient.get('/api/integrity/gaps', { params })
    return response.data
  },

  /**
   * 触发扫描
   */
  async triggerScan(params: {
    symbol: string
    interval: string
    autoRepair?: boolean
  }): Promise<any> {
    const response = await apiClient.post('/api/integrity/scan', null, { params })
    return response.data
  }
}

/**
 * 回补任务 API
 */
export const backfillApi = {
  /**
   * 创建回补任务
   */
  async createTask(data: {
    symbol: string
    interval: string
    startTime: number
    endTime: number
    autoExecute?: boolean
  }): Promise<any> {
    const response = await apiClient.post('/api/backfill', data)
    return response.data
  },

  /**
   * 获取任务状态
   */
  async getTask(taskId: number): Promise<any> {
    const response = await apiClient.get(`/api/backfill/${taskId}`)
    return response.data
  },

  /**
   * 获取所有任务
   */
  async getAllTasks(): Promise<any[]> {
    const response = await apiClient.get('/api/backfill')
    return response.data
  },

  /**
   * 取消任务
   */
  async cancelTask(taskId: number): Promise<void> {
    await apiClient.delete(`/api/backfill/${taskId}`)
  },

  /**
   * 获取任务批次
   */
  async getTaskBatches(taskId: number): Promise<any[]> {
    const response = await apiClient.get(`/api/backfill/${taskId}/batches`)
    return response.data
  },

  /**
   * 获取失败批次
   */
  async getFailedBatches(taskId: number): Promise<any[]> {
    const response = await apiClient.get(`/api/backfill/${taskId}/batches/failed`)
    return response.data
  },

  /**
   * 重试失败批次
   */
  async retryFailedBatches(taskId: number): Promise<any> {
    const response = await apiClient.post(`/api/backfill/${taskId}/retry`)
    return response.data
  },

  /**
   * 重试单个批次
   */
  async retryBatch(batchId: number): Promise<any> {
    const response = await apiClient.post(`/api/backfill/batches/${batchId}/retry`)
    return response.data
  }
}

/**
 * 延迟统计 API
 */
export const latencyStatsApi = {
  /**
   * 获取所有统计
   */
  async getAllStats(): Promise<any[]> {
    const response = await apiClient.get('/api/stats/latency')
    return response.data
  },

  /**
   * 获取指定交易所和端点的统计
   */
  async getStats(exchange: string, endpoint: string): Promise<any> {
    const response = await apiClient.get('/api/stats/latency/query', {
      params: { exchange, endpoint }
    })
    return response.data
  },

  /**
   * 获取指定交易所的统计
   */
  async getStatsByExchange(exchange: string): Promise<any[]> {
    const response = await apiClient.get(`/api/stats/latency/exchange/${exchange}`)
    return response.data
  },

  /**
   * 获取最近统计
   */
  async getRecentStats(seconds: number = 300): Promise<any[]> {
    const response = await apiClient.get('/api/stats/latency/recent', {
      params: { seconds }
    })
    return response.data
  },

  /**
   * 清除统计
   */
  async clearStats(exchange?: string): Promise<any> {
    const response = await apiClient.delete('/api/stats/latency', {
      params: exchange ? { exchange } : undefined
    })
    return response.data
  }
}

/**
 * 交易所配置 API
 */
export const exchangeConfigApi = {
  /**
   * 获取所有配置
   */
  async getAllConfigs(): Promise<any[]> {
    const response = await apiClient.get('/api/exchange-configs')
    return response.data
  },

  /**
   * 获取指定配置
   */
  async getConfig(name: string): Promise<any> {
    const response = await apiClient.get(`/api/exchange-configs/${name}`)
    return response.data
  },

  /**
   * 保存配置
   */
  async saveConfig(data: any): Promise<any> {
    const response = await apiClient.post('/api/exchange-configs', data)
    return response.data
  },

  /**
   * 更新配置
   */
  async updateConfig(name: string, data: any): Promise<any> {
    const response = await apiClient.put(`/api/exchange-configs/${name}`, data)
    return response.data
  },

  /**
   * 删除配置
   */
  async deleteConfig(name: string): Promise<void> {
    await apiClient.delete(`/api/exchange-configs/${name}`)
  }
}

export default apiClient
