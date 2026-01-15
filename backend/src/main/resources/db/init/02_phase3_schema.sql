-- ============================================
-- Phase 3: Strategy and Backtest Schema
-- ============================================

-- ============================================
-- Strategies Table
-- ============================================
CREATE TABLE IF NOT EXISTS strategies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(20) DEFAULT '1.0.0',
    
    -- Applicable levels (PostgreSQL array)
    levels VARCHAR(10)[] NOT NULL DEFAULT ARRAY['1m', '5m', '15m'],
    
    -- Conditions stored as JSONB for flexibility
    entry_conditions JSONB NOT NULL DEFAULT '{}',
    exit_conditions JSONB NOT NULL DEFAULT '{}',
    position_sizing JSONB NOT NULL DEFAULT '{}',
    risk_control JSONB NOT NULL DEFAULT '{}',
    
    -- Auto trading flag (Phase 4)
    auto_trade BOOLEAN DEFAULT FALSE,
    
    -- Status: active, inactive, archived
    status VARCHAR(20) DEFAULT 'active',
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_strategies_status ON strategies(status);
CREATE INDEX IF NOT EXISTS idx_strategies_name ON strategies(name);

-- ============================================
-- Signals Table (Trading Signals)
-- ============================================
CREATE TABLE IF NOT EXISTS signals (
    id BIGSERIAL PRIMARY KEY,
    strategy_id BIGINT REFERENCES strategies(id) ON DELETE SET NULL,
    
    -- Signal details
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    signal_type VARCHAR(10) NOT NULL,  -- 'buy' or 'sell'
    level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 3),  -- 1st, 2nd, 3rd level
    
    -- Price targets
    entry_price DECIMAL(20, 8) NOT NULL,
    stop_loss DECIMAL(20, 8),
    take_profit DECIMAL(20, 8),
    
    -- Signal quality
    confidence VARCHAR(10) NOT NULL CHECK (confidence IN ('high', 'medium', 'low')),
    reason TEXT,
    
    -- Related chan structure IDs
    fenxing_id BIGINT,
    bi_id BIGINT,
    xianduan_id BIGINT,
    zhongshu_id BIGINT,
    
    -- Status: pending, confirmed, executed, cancelled, expired
    status VARCHAR(20) DEFAULT 'pending',
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Convert to hypertable for time-series optimization
SELECT create_hypertable('signals', 'created_at', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_signals_symbol ON signals(symbol, interval);
CREATE INDEX IF NOT EXISTS idx_signals_strategy ON signals(strategy_id);
CREATE INDEX IF NOT EXISTS idx_signals_status ON signals(status);
CREATE INDEX IF NOT EXISTS idx_signals_type ON signals(signal_type, confidence);

-- ============================================
-- Backtest Tasks Table
-- ============================================
CREATE TABLE IF NOT EXISTS backtest_tasks (
    id BIGSERIAL PRIMARY KEY,
    strategy_id BIGINT NOT NULL REFERENCES strategies(id) ON DELETE CASCADE,
    
    -- Backtest parameters
    symbols VARCHAR(20)[] NOT NULL,
    intervals VARCHAR(10)[] NOT NULL DEFAULT ARRAY['1m'],
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    initial_capital DECIMAL(20, 8) NOT NULL DEFAULT 10000,
    
    -- Simulation parameters
    slippage DECIMAL(10, 6) DEFAULT 0.001,  -- 0.1%
    commission DECIMAL(10, 6) DEFAULT 0.001,  -- 0.1%
    
    -- Status: pending, running, completed, failed, cancelled
    status VARCHAR(20) DEFAULT 'pending',
    progress INTEGER DEFAULT 0,
    error_message TEXT,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_backtest_tasks_strategy ON backtest_tasks(strategy_id);
CREATE INDEX IF NOT EXISTS idx_backtest_tasks_status ON backtest_tasks(status);

-- ============================================
-- Backtest Results Table
-- ============================================
CREATE TABLE IF NOT EXISTS backtest_results (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES backtest_tasks(id) ON DELETE CASCADE,
    
    -- Return metrics
    total_return DECIMAL(10, 4),           -- Total return percentage
    annualized_return DECIMAL(10, 4),      -- Annualized return percentage
    
    -- Risk metrics
    max_drawdown DECIMAL(10, 4),           -- Maximum drawdown percentage
    sharpe_ratio DECIMAL(10, 4),           -- Sharpe ratio
    sortino_ratio DECIMAL(10, 4),          -- Sortino ratio
    calmar_ratio DECIMAL(10, 4),           -- Calmar ratio
    volatility DECIMAL(10, 4),             -- Annualized volatility
    
    -- Trade metrics
    total_trades INTEGER DEFAULT 0,
    winning_trades INTEGER DEFAULT 0,
    losing_trades INTEGER DEFAULT 0,
    win_rate DECIMAL(10, 4),               -- Win rate percentage
    profit_factor DECIMAL(10, 4),          -- Profit factor
    average_win DECIMAL(20, 8),            -- Average winning trade
    average_loss DECIMAL(20, 8),           -- Average losing trade
    max_consecutive_losses INTEGER,         -- Maximum consecutive losses
    
    -- Time metrics
    average_holding_time BIGINT,           -- Average holding time in milliseconds
    trading_frequency DECIMAL(10, 4),      -- Trades per day
    
    -- Final state
    final_equity DECIMAL(20, 8),
    peak_equity DECIMAL(20, 8),
    
    -- Detailed data (JSONB)
    equity_curve JSONB,                    -- Array of {timestamp, equity, drawdown}
    monthly_returns JSONB,                 -- Object of {YYYY-MM: return_percentage}
    trades JSONB,                          -- Array of trade records
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_backtest_results_task ON backtest_results(task_id);

-- ============================================
-- Strategy Templates Table (Predefined strategies)
-- ============================================
CREATE TABLE IF NOT EXISTS strategy_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),  -- 'trend', 'reversal', 'scalping', 'swing'
    
    -- Template configuration
    levels VARCHAR(10)[] NOT NULL,
    entry_conditions JSONB NOT NULL,
    exit_conditions JSONB NOT NULL,
    position_sizing JSONB NOT NULL,
    risk_control JSONB NOT NULL,
    
    -- Template metadata
    is_system BOOLEAN DEFAULT TRUE,  -- System template or user-created
    usage_count INTEGER DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_strategy_templates_category ON strategy_templates(category);

-- ============================================
-- Insert default strategy templates
-- ============================================
INSERT INTO strategy_templates (name, description, category, levels, entry_conditions, exit_conditions, position_sizing, risk_control) VALUES
(
    '多级别共振策略',
    '小级别买点 + 中级别上涨趋势 + 大级别突破中枢的多级别共振策略',
    'trend',
    ARRAY['1m', '5m', '15m'],
    '{
        "primary": {
            "level": "1m",
            "conditions": [{"type": "trading-point", "level": 1, "direction": "buy", "minConfidence": "medium"}],
            "logic": "AND"
        },
        "secondary": {
            "level": "5m",
            "conditions": [{"type": "trend", "direction": "up", "minXianduanCount": 2}],
            "logic": "AND"
        }
    }'::jsonb,
    '{
        "stopLoss": {"type": "fenxing", "offset": -0.005},
        "takeProfit": {"type": "ratio", "ratio": 2}
    }'::jsonb,
    '{"type": "fixed-risk", "riskPerTrade": 0.02}'::jsonb,
    '{"maxDailyLoss": 0.05, "maxDrawdown": 0.15, "maxPositions": 3}'::jsonb
),
(
    '背驰反转策略',
    '趋势末端出现背驰信号时的反转策略',
    'reversal',
    ARRAY['5m', '15m'],
    '{
        "primary": {
            "level": "5m",
            "conditions": [
                {"type": "divergence", "indicator": "macd", "divergenceType": "bullish", "minStrength": "medium"},
                {"type": "trading-point", "level": 2, "direction": "buy"}
            ],
            "logic": "AND"
        }
    }'::jsonb,
    '{
        "stopLoss": {"type": "zhongshu", "offset": -0.01},
        "takeProfit": {"type": "target", "targetPrice": "zhongshu_high"}
    }'::jsonb,
    '{"type": "fixed", "value": 0.1}'::jsonb,
    '{"maxDailyLoss": 0.03, "maxDrawdown": 0.1, "maxPositions": 2}'::jsonb
)
ON CONFLICT DO NOTHING;

-- ============================================
-- Comments for documentation
-- ============================================
COMMENT ON TABLE strategies IS 'User-defined trading strategies with entry/exit conditions';
COMMENT ON TABLE signals IS 'Generated trading signals based on Chan Theory analysis';
COMMENT ON TABLE backtest_tasks IS 'Backtest execution tasks for strategy validation';
COMMENT ON TABLE backtest_results IS 'Detailed backtest results with performance metrics';
COMMENT ON TABLE strategy_templates IS 'Predefined strategy templates for quick setup';
