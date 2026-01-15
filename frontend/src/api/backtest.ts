import apiClient from './market'

/**
 * 回测任务接口
 */
export interface BacktestTask {
  id?: number
  strategyId: number
  symbols: string
  intervals?: string
  startTime: string
  endTime: string
  initialCapital?: string
  slippage?: string
  commission?: string
  status?: string
  progress?: number
  errorMessage?: string
  createdAt?: string
  startedAt?: string
  completedAt?: string
}

/**
 * 回测结果接口
 */
export interface BacktestResult {
  id?: number
  taskId: number

  // Return metrics
  totalReturn?: string
  annualizedReturn?: string

  // Risk metrics
  maxDrawdown?: string
  sharpeRatio?: string
  sortinoRatio?: string
  calmarRatio?: string
  volatility?: string

  // Trade metrics
  totalTrades?: number
  winningTrades?: number
  losingTrades?: number
  winRate?: string
  profitFactor?: string
  averageWin?: string
  averageLoss?: string
  maxConsecutiveLosses?: number

  // Time metrics
  averageHoldingTime?: number
  tradingFrequency?: string

  // Final state
  finalEquity?: string
  peakEquity?: string

  // Detailed data (JSON strings)
  equityCurve?: string
  monthlyReturns?: string
  trades?: string

  createdAt?: string
}

/**
 * 回测统计接口
 */
export interface BacktestStats {
  pending: number
  running: number
  completed: number
  failed: number
  cancelled: number
}

/**
 * 回测 API
 */
export const backtestApi = {
  /**
   * 创建回测任务
   */
  async createTask(data: BacktestTask): Promise<BacktestTask> {
    const response = await apiClient.post('/api/backtest/tasks', data)
    return response.data
  },

  /**
   * 获取回测任务
   */
  async getTask(id: number): Promise<BacktestTask> {
    const response = await apiClient.get(`/api/backtest/tasks/${id}`)
    return response.data
  },

  /**
   * 获取回测任务列表
   */
  async getTasks(params?: {
    strategyId?: number
    status?: string
  }): Promise<BacktestTask[]> {
    const response = await apiClient.get('/api/backtest/tasks', { params })
    return response.data
  },

  /**
   * 获取待执行任务
   */
  async getPendingTasks(): Promise<BacktestTask[]> {
    const response = await apiClient.get('/api/backtest/tasks/pending')
    return response.data
  },

  /**
   * 启动回测任务
   */
  async startTask(id: number): Promise<BacktestTask> {
    const response = await apiClient.post(`/api/backtest/tasks/${id}/start`)
    return response.data
  },

  /**
   * 取消回测任务
   */
  async cancelTask(id: number): Promise<BacktestTask> {
    const response = await apiClient.post(`/api/backtest/tasks/${id}/cancel`)
    return response.data
  },

  /**
   * 删除回测任务
   */
  async deleteTask(id: number): Promise<void> {
    await apiClient.delete(`/api/backtest/tasks/${id}`)
  },

  /**
   * 获取回测结果
   */
  async getResult(taskId: number): Promise<BacktestResult> {
    const response = await apiClient.get(`/api/backtest/results/${taskId}`)
    return response.data
  },

  /**
   * 获取回测统计
   */
  async getStats(): Promise<BacktestStats> {
    const response = await apiClient.get('/api/backtest/stats')
    return response.data
  }
}
