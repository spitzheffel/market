# Phase 3 Quick Start Guide

This guide will help you quickly get started with Phase 3 features: Strategies, Signals, and Backtesting.

---

## 🚀 Quick Start

### Step 1: Start the Application

```bash
# Start database
docker-compose up -d

# Start backend
cd backend
mvn spring-boot:run

# Start frontend
cd frontend
npm run dev
```

### Step 2: Verify Phase 3 is Running

Check that the signal generator is running by looking at the logs:

```
[SignalGeneratorService] Checking signals for X active strategies
```

---

## 📝 Create Your First Strategy

### Option 1: Use API

```bash
curl -X POST http://localhost:8080/api/strategies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Simple Buy Point Strategy",
    "description": "Buy on 1st level buy points with high confidence",
    "levels": "1m,5m",
    "entryConditions": "{\"primary\":{\"level\":\"1m\",\"conditions\":[{\"type\":\"trading-point\",\"level\":1,\"direction\":\"buy\",\"minConfidence\":\"high\"}],\"logic\":\"AND\"}}",
    "exitConditions": "{\"stopLoss\":{\"type\":\"percentage\",\"value\":0.02},\"takeProfit\":{\"type\":\"ratio\",\"ratio\":2}}",
    "positionSizing": "{\"type\":\"fixed-risk\",\"riskPerTrade\":0.02}",
    "riskControl": "{\"maxPositions\":3,\"maxDrawdown\":0.15}",
    "status": "active"
  }'
```

### Option 2: Use Database Template

The system comes with 2 pre-configured strategy templates:

1. **多级别共振策略** (Multi-Level Resonance)
   - Small level buy point + Medium level uptrend + Large level breakout

2. **背驰反转策略** (Divergence Reversal)
   - Trend exhaustion with MACD divergence

To use a template, copy it to the strategies table:

```sql
INSERT INTO strategies (name, description, levels, entry_conditions, exit_conditions, position_sizing, risk_control, status)
SELECT name, description, levels, entry_conditions, exit_conditions, position_sizing, risk_control, 'active'
FROM strategy_templates
WHERE id = 1;
```

---

## 🔔 Monitor Signals

### View All Signals

```bash
curl http://localhost:8080/api/signals
```

### View Pending Signals

```bash
curl http://localhost:8080/api/signals/pending
```

### View High Confidence Buy Signals

```bash
curl http://localhost:8080/api/signals/high-confidence?type=buy
```

### Signal Statistics

```bash
curl http://localhost:8080/api/signals/stats
```

Response:
```json
{
  "buySignals": 15,
  "sellSignals": 8,
  "highConfidence": 5,
  "mediumConfidence": 12,
  "lowConfidence": 6
}
```

---

## 📊 Run Your First Backtest

### Step 1: Create Backtest Task

```bash
curl -X POST http://localhost:8080/api/backtest/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "symbols": "BTCUSDT",
    "intervals": "1m",
    "startTime": "2024-01-01T00:00:00Z",
    "endTime": "2024-01-07T23:59:59Z",
    "initialCapital": "10000",
    "slippage": "0.001",
    "commission": "0.001"
  }'
```

Response:
```json
{
  "id": 1,
  "strategyId": 1,
  "symbols": "BTCUSDT",
  "status": "pending",
  "progress": 0,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### Step 2: Start Backtest

```bash
curl -X POST http://localhost:8080/api/backtest/tasks/1/start
```

### Step 3: Monitor Progress

```bash
# Check task status
curl http://localhost:8080/api/backtest/tasks/1
```

Response:
```json
{
  "id": 1,
  "status": "running",
  "progress": 45,
  "startedAt": "2024-01-15T10:01:00Z"
}
```

### Step 4: Get Results

Once status is "completed":

```bash
curl http://localhost:8080/api/backtest/results/1
```

Response:
```json
{
  "taskId": 1,
  "totalReturn": "15.50",
  "annualizedReturn": "812.60",
  "maxDrawdown": "8.20",
  "sharpeRatio": "2.45",
  "sortinoRatio": "3.12",
  "winRate": "65.00",
  "profitFactor": "2.15",
  "totalTrades": 23,
  "winningTrades": 15,
  "losingTrades": 8,
  "finalEquity": "11550.00"
}
```

---

## 🎯 Understanding Strategy Conditions

### Entry Conditions Structure

```json
{
  "primary": {
    "level": "1m",
    "conditions": [
      {
        "type": "trading-point",
        "level": 1,
        "direction": "buy",
        "minConfidence": "high"
      }
    ],
    "logic": "AND"
  },
  "secondary": {
    "level": "5m",
    "conditions": [
      {
        "type": "trend",
        "direction": "up",
        "minXianduanCount": 2
      }
    ],
    "logic": "AND"
  },
  "requireResonance": true
}
```

### Condition Types

1. **trading-point**: Buy/sell points from Chan analysis
   ```json
   {
     "type": "trading-point",
     "level": 1,              // 1, 2, or 3
     "direction": "buy",      // "buy" or "sell"
     "minConfidence": "high"  // "high", "medium", "low"
   }
   ```

2. **trend**: Trend direction based on Xianduans
   ```json
   {
     "type": "trend",
     "direction": "up",       // "up" or "down"
     "minXianduanCount": 2    // Minimum number of xianduans
   }
   ```

3. **divergence**: MACD or price divergence
   ```json
   {
     "type": "divergence",
     "indicator": "macd",
     "divergenceType": "bullish",
     "minStrength": "medium"
   }
   ```

4. **zhongshu**: Central hub conditions
   ```json
   {
     "type": "zhongshu",
     "action": "breakout",    // "breakout", "inside", "bounce"
     "direction": "up"
   }
   ```

5. **price**: Price-based conditions
   ```json
   {
     "type": "price",
     "operator": "above",     // "above", "below", "between"
     "referenceType": "zhongshu_high",
     "offset": 0.01
   }
   ```

### Exit Conditions Structure

```json
{
  "stopLoss": {
    "type": "percentage",    // "percentage", "fenxing", "zhongshu"
    "value": 0.02           // 2% stop loss
  },
  "takeProfit": {
    "type": "ratio",        // "ratio", "target", "trailing"
    "ratio": 2              // 2:1 risk-reward ratio
  }
}
```

---

## 🔧 Configuration Tips

### 1. Adjust Signal Generation Frequency

Edit `SignalGeneratorService.java`:

```java
@Scheduled(fixedDelay = 60000)  // 60 seconds (default)
```

Change to:
```java
@Scheduled(fixedDelay = 300000) // 5 minutes
```

### 2. Add More Monitored Symbols

Edit `SignalGeneratorService.java`, line 67:

```java
String[] symbols = {"BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT"};
```

### 3. Adjust Backtest Parameters

When creating a backtest task:

```json
{
  "initialCapital": "10000",    // Starting capital
  "slippage": "0.001",          // 0.1% slippage
  "commission": "0.001"         // 0.1% commission per trade
}
```

### 4. Enable/Disable Strategies

```bash
# Activate strategy
curl -X PUT http://localhost:8080/api/strategies/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "active"}'

# Deactivate strategy
curl -X PUT http://localhost:8080/api/strategies/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "inactive"}'
```

---

## 📈 Interpreting Backtest Results

### Good Performance Indicators

- **Total Return**: > 10% for short-term, > 50% for long-term
- **Sharpe Ratio**: > 1.0 (good), > 2.0 (excellent)
- **Max Drawdown**: < 20% (acceptable), < 10% (good)
- **Win Rate**: > 50% (profitable), > 60% (good)
- **Profit Factor**: > 1.5 (good), > 2.0 (excellent)

### Red Flags

- ⚠️ Max Drawdown > 30%
- ⚠️ Sharpe Ratio < 0.5
- ⚠️ Win Rate < 40%
- ⚠️ Profit Factor < 1.2
- ⚠️ Max Consecutive Losses > 10

---

## 🐛 Troubleshooting

### No Signals Generated

**Check**:
1. Are there active strategies? `curl http://localhost:8080/api/strategies?status=active`
2. Is the scheduler running? Check logs for "Checking signals for X active strategies"
3. Is there K-line data? `curl http://localhost:8080/api/klines?symbol=BTCUSDT&interval=1m&limit=10`

### Backtest Fails

**Common Issues**:
1. **No K-line data**: Ensure data exists for the backtest period
2. **Invalid strategy conditions**: Check JSON syntax in entry/exit conditions
3. **Strategy not found**: Verify strategyId exists

**Check logs**:
```bash
tail -f backend/logs/spring.log | grep -i backtest
```

### Frontend Not Showing Data

**Check**:
1. Backend is running: `curl http://localhost:8080/api/strategies`
2. CORS is configured correctly
3. API base URL is correct in `frontend/src/api/market.ts`

---

## 📚 Next Steps

1. **Create Custom Strategies**: Experiment with different condition combinations
2. **Run Multiple Backtests**: Test strategies across different time periods
3. **Analyze Results**: Compare performance metrics across strategies
4. **Optimize Parameters**: Adjust stop loss, take profit, position sizing
5. **Monitor Live Signals**: Watch for high-confidence signals
6. **Prepare for Phase 4**: Auto-trading implementation

---

## 🆘 Need Help?

- Check logs: `backend/logs/spring.log`
- Review API documentation: `docs/PHASE3_IMPLEMENTATION_SUMMARY.md`
- Test with curl commands above
- Verify database schema: `backend/src/main/resources/db/init/02_phase3_schema.sql`

---

**Happy Trading! 🚀**
