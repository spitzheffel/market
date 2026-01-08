<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>市场</h2>
        <div class="panel-sub">按交易所 / 周期筛选，查看实时行情。</div>
      </div>
      <div class="flex flex-wrap gap-2">
        <button class="select-pill">Binance</button>
        <button class="select-pill">现货</button>
        <button class="select-pill">1m</button>
        <button class="button-ghost">刷新</button>
      </div>
    </div>
    <div class="grid gap-3 md:grid-cols-3">
      <div class="mini-card p-3">
        涨幅前列
        <strong class="mono">{{ topMovers[0]?.pair }}</strong>
        <div class="panel-sub">+{{ topMovers[0]?.change }}%</div>
      </div>
      <div class="mini-card p-3">
        成交额(24h)
        <strong class="mono">{{ volume24h }}</strong>
        <div class="panel-sub">USDT</div>
      </div>
      <div class="mini-card p-3">
        扫描间隔
        <strong class="mono">5s</strong>
        <div class="panel-sub">实时推送</div>
      </div>
    </div>
    <div class="overflow-x-auto">
      <table>
        <thead>
          <tr>
            <th>交易对</th>
            <th>最新价</th>
            <th>涨跌</th>
            <th>周期</th>
            <th>信号</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in markets" :key="row.pair">
            <td>{{ row.pair }}</td>
            <td class="mono">{{ row.last }}</td>
            <td :class="['mono', row.change.startsWith('-') ? 'text-danger' : 'text-success']">{{ row.change }}%</td>
            <td>{{ row.interval }}</td>
            <td><span :class="['tag', row.signal]">{{ row.signalLabel }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
const markets = [
  { pair: 'BTC/USDT', last: '67,420.5', change: '+2.4', interval: '1m', signal: 'buy', signalLabel: '买' },
  { pair: 'ETH/USDT', last: '3,492.1', change: '+1.2', interval: '1m', signal: 'wait', signalLabel: '观望' },
  { pair: 'SOL/USDT', last: '142.8', change: '+0.3', interval: '5m', signal: 'buy', signalLabel: '买' },
  { pair: 'BNB/USDT', last: '612.4', change: '-0.9', interval: '15m', signal: 'sell', signalLabel: '卖' },
];

const topMovers = markets.slice(0, 1);
const volume24h = '1.28B';
</script>
