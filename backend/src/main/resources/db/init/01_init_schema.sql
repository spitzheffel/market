-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================
-- K-Line Data Table (Time Series)
-- ============================================
CREATE TABLE IF NOT EXISTS klines (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    open DECIMAL(20, 8) NOT NULL,
    high DECIMAL(20, 8) NOT NULL,
    low DECIMAL(20, 8) NOT NULL,
    close DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(20, 8) NOT NULL,
    PRIMARY KEY (symbol, interval, time)
);

-- Convert to hypertable (TimescaleDB feature)
SELECT create_hypertable('klines', 'time', if_not_exists => TRUE);

-- Create index for common queries
CREATE INDEX IF NOT EXISTS idx_klines_symbol_interval_time ON klines(symbol, interval, time DESC);

-- ============================================
-- Chan Theory Results Table
-- ============================================
CREATE TABLE IF NOT EXISTS chan_results (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    result_type VARCHAR(20) NOT NULL,  -- fenxing, bi, xianduan, zhongshu
    result JSONB NOT NULL,
    calculated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_chan_results UNIQUE (symbol, interval, result_type)
);

CREATE INDEX IF NOT EXISTS idx_chan_results_symbol ON chan_results(symbol, interval);

-- ============================================
-- Exchange Configuration Table
-- ============================================
CREATE TABLE IF NOT EXISTS exchange_configs (
    name VARCHAR(20) PRIMARY KEY,
    enabled BOOLEAN DEFAULT TRUE,
    api_key TEXT,
    api_secret TEXT,
    config JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- Backfill Task Table
-- ============================================
CREATE TABLE IF NOT EXISTS backfill_tasks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',  -- pending, running, completed, failed
    progress INTEGER DEFAULT 0,
    total_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_backfill_tasks_status ON backfill_tasks(status);

-- ============================================
-- Backfill Task Batches Table
-- ============================================
CREATE TABLE IF NOT EXISTS backfill_task_batches (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES backfill_tasks(id),
    batch_index INTEGER NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, RUNNING, COMPLETED, FAILED
    record_count INTEGER DEFAULT 0,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_backfill_batches_task ON backfill_task_batches(task_id);
CREATE INDEX IF NOT EXISTS idx_backfill_batches_status ON backfill_task_batches(status);

-- ============================================
-- Insert default exchange configs
-- ============================================
INSERT INTO exchange_configs (name, enabled, config) VALUES
('binance', true, '{"baseUrl": "https://api.binance.com", "wsUrl": "wss://stream.binance.com:9443/ws"}'),
('okx', false, '{"baseUrl": "https://www.okx.com"}'),
('bybit', false, '{"baseUrl": "https://api.bybit.com"}')
ON CONFLICT (name) DO NOTHING;
