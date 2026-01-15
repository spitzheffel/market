import apiClient from './market'

/**
 * 策略接口
 */
export interface Strategy {
  id?: number
  name: string
  description?: string
  version?: string
  levels: string
  entryConditions: string
  exitConditions: string
  positionSizing: string
  riskControl: string
  autoTrade?: boolean
  status?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 策略 API
 */
export const strategyApi = {
  /**
   * 获取所有策略
   */
  async getStrategies(params?: {
    status?: string
    name?: string
    level?: string
  }): Promise<Strategy[]> {
    const response = await apiClient.get('/api/strategies', { params })
    return response.data
  },

  /**
   * 获取策略详情
   */
  async getStrategy(id: number): Promise<Strategy> {
    const response = await apiClient.get(`/api/strategies/${id}`)
    return response.data
  },

  /**
   * 创建策略
   */
  async createStrategy(data: Strategy): Promise<Strategy> {
    const response = await apiClient.post('/api/strategies', data)
    return response.data
  },

  /**
   * 更新策略
   */
  async updateStrategy(id: number, data: Partial<Strategy>): Promise<Strategy> {
    const response = await apiClient.put(`/api/strategies/${id}`, data)
    return response.data
  },

  /**
   * 删除策略
   */
  async deleteStrategy(id: number): Promise<void> {
    await apiClient.delete(`/api/strategies/${id}`)
  },

  /**
   * 归档策略
   */
  async archiveStrategy(id: number): Promise<Strategy> {
    const response = await apiClient.post(`/api/strategies/${id}/archive`)
    return response.data
  },

  /**
   * 获取自动交易策略
   */
  async getAutoTradeStrategies(): Promise<Strategy[]> {
    const response = await apiClient.get('/api/strategies/auto-trade')
    return response.data
  }
}
