import apiClient from './market'

/**
 * 信号接口
 */
export interface Signal {
  id?: number
  strategyId?: number
  symbol: string
  interval: string
  signalType: 'buy' | 'sell'
  level: number
  entryPrice: string
  stopLoss?: string
  takeProfit?: string
  confidence: 'high' | 'medium' | 'low'
  reason?: string
  fenxingId?: number
  biId?: number
  xianduanId?: number
  zhongshuId?: number
  status?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 信号统计接口
 */
export interface SignalStats {
  buySignals: number
  sellSignals: number
  highConfidence: number
  mediumConfidence: number
  lowConfidence: number
}

/**
 * 分页信号响应
 */
export interface SignalPage {
  content: Signal[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/**
 * 信号 API
 */
export const signalApi = {
  /**
   * 获取信号列表
   */
  async getSignals(params?: {
    symbol?: string
    interval?: string
    strategyId?: number
  }): Promise<Signal[]> {
    const response = await apiClient.get('/api/signals', { params })
    return response.data
  },

  /**
   * 获取分页信号
   */
  async getSignalsPaged(params: {
    symbol: string
    page?: number
    size?: number
  }): Promise<SignalPage> {
    const response = await apiClient.get('/api/signals/paged', { params })
    return response.data
  },

  /**
   * 获取待处理信号
   */
  async getPendingSignals(symbol?: string): Promise<Signal[]> {
    const response = await apiClient.get('/api/signals/pending', {
      params: symbol ? { symbol } : undefined
    })
    return response.data
  },

  /**
   * 获取高置信度信号
   */
  async getHighConfidenceSignals(type: 'buy' | 'sell' = 'buy'): Promise<Signal[]> {
    const response = await apiClient.get('/api/signals/high-confidence', {
      params: { type }
    })
    return response.data
  },

  /**
   * 获取信号详情
   */
  async getSignal(id: number): Promise<Signal> {
    const response = await apiClient.get(`/api/signals/${id}`)
    return response.data
  },

  /**
   * 创建信号
   */
  async createSignal(data: Signal): Promise<Signal> {
    const response = await apiClient.post('/api/signals', data)
    return response.data
  },

  /**
   * 确认信号
   */
  async confirmSignal(id: number): Promise<Signal> {
    const response = await apiClient.post(`/api/signals/${id}/confirm`)
    return response.data
  },

  /**
   * 取消信号
   */
  async cancelSignal(id: number): Promise<Signal> {
    const response = await apiClient.post(`/api/signals/${id}/cancel`)
    return response.data
  },

  /**
   * 标记信号为已执行
   */
  async executeSignal(id: number): Promise<Signal> {
    const response = await apiClient.post(`/api/signals/${id}/execute`)
    return response.data
  },

  /**
   * 获取信号统计
   */
  async getSignalStats(): Promise<SignalStats> {
    const response = await apiClient.get('/api/signals/stats')
    return response.data
  }
}
