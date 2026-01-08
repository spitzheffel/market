<template>
  <section class="chart-card card flex flex-col gap-4 p-5">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>结构叠加</h2>
        <div class="panel-sub">笔、线段、中枢、背驰映射。</div>
      </div>
      <div class="panel-actions flex gap-2">
        <button class="button-ghost">叠加：开启</button>
        <button class="button-ghost">自动缩放</button>
      </div>
    </div>
    <div class="chart-shell grid grid-rows-[1fr_auto] gap-3 p-4">
      <div class="chart-grid grid grid-cols-1 xl:grid-cols-[1fr_200px] gap-4">
        <div class="chart-visual rounded-xl border border-[rgba(148,163,184,0.25)] relative overflow-hidden p-3.5">
          <svg viewBox="0 0 600 280" preserveAspectRatio="none" aria-hidden="true">
            <rect x="0" y="0" width="600" height="280" fill="none" />
            <path
              d="M20 210 L70 170 L120 200 L170 120 L220 150 L270 90 L320 130 L370 80 L420 120 L470 70 L520 110 L580 50"
              stroke="#60a5fa"
              stroke-width="2.5"
              fill="none"
            />
            <path
              d="M20 220 L70 190 L120 220 L170 160 L220 180 L270 120 L320 150 L370 110 L420 150 L470 100 L520 140 L580 90"
              stroke="#22d3ee"
              stroke-width="2"
              stroke-dasharray="6 6"
              fill="none"
            />
            <rect x="120" y="140" width="120" height="70" fill="rgba(245, 158, 11, 0.18)" stroke="#f59e0b" />
            <rect x="310" y="95" width="120" height="70" fill="rgba(245, 158, 11, 0.18)" stroke="#f59e0b" />
            <g fill="#f43f5e">
              <rect x="70" y="150" width="8" height="40" />
              <rect x="140" y="170" width="8" height="30" />
              <rect x="210" y="110" width="8" height="55" />
            </g>
            <g fill="#22c55e">
              <rect x="95" y="140" width="8" height="55" />
              <rect x="180" y="120" width="8" height="45" />
              <rect x="260" y="95" width="8" height="60" />
            </g>
          </svg>
        </div>
        <div class="chart-side flex flex-col gap-3">
          <div v-for="stat in miniStats" :key="stat.label" class="mini-card p-3">
            {{ stat.label }}
            <strong class="mono">{{ stat.value }}</strong>
            <div class="panel-sub">{{ stat.note }}</div>
          </div>
        </div>
      </div>
      <div class="chart-legend flex flex-wrap gap-3 text-sm text-muted">
        <span><span class="legend-dot" style="background: #60a5fa"></span>价格</span>
        <span><span class="legend-dot" style="background: #22d3ee"></span>线段</span>
        <span><span class="legend-dot" style="background: #f59e0b"></span>中枢</span>
        <span><span class="legend-dot" style="background: #f43f5e"></span>卖点</span>
      </div>
    </div>
    <div class="chart-footer grid grid-cols-2 md:grid-cols-4 gap-3 text-xs text-muted">
      <div v-for="item in chartFooter" :key="item.label">
        {{ item.label }}: <span class="mono">{{ item.value }}</span>
      </div>
    </div>
  </section>

  <section class="levels-row grid gap-4 xl:grid-cols-[1.2fr_1fr]">
    <div class="levels-card card flex flex-col gap-3 p-4">
      <div class="panel-header flex items-center justify-between gap-4">
        <div>
          <h2>多级别快照</h2>
          <div class="panel-sub">1m / 5m / 15m 多周期联动</div>
        </div>
        <button class="button-ghost">锁定联动</button>
      </div>
      <div class="level-grid grid grid-cols-1 md:grid-cols-3 gap-3">
        <div v-for="level in levels" :key="level.level" class="level-item">
          <h4>{{ level.level }}</h4>
          <strong>{{ level.title }}</strong>
          <div class="panel-sub">{{ level.note }}</div>
        </div>
      </div>
      <div class="panel-sub">共识：买方区间形成（弱）。</div>
    </div>
    <div class="watch-card card flex flex-col gap-3 p-4">
      <div class="panel-header flex items-center justify-between gap-4">
        <div>
          <h2>自选池</h2>
          <div class="panel-sub">自动扫描缠论信号</div>
        </div>
        <button class="button-ghost">编辑</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>交易对</th>
            <th>最新价</th>
            <th>信号</th>
            <th>偏向</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in watchlist" :key="row.pair">
            <td>{{ row.pair }}</td>
            <td class="mono">{{ row.last }}</td>
            <td><span :class="['tag', row.signal]">{{ row.signalLabel }}</span></td>
            <td class="mono">{{ row.bias }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
const miniStats = [
  { label: '突破力度', value: '0.78', note: '线段 3 对比 1' },
  { label: '背驰指数', value: '+12.4', note: '动能减弱' },
  { label: '中枢同步', value: '3 / 4', note: '多级别对齐' },
];

const chartFooter = [
  { label: '结构更新时间', value: '00:00:47' },
  { label: '最新确认笔', value: 'S-28' },
  { label: '线段斜率', value: '+1.8%' },
  { label: '中枢重叠', value: '0.62' },
];

const levels = [
  { level: '1m', title: '笔 42', note: '中枢 6 | 趋势上' },
  { level: '5m', title: '线段 12', note: '中枢 3 | 趋势盘整' },
  { level: '15m', title: '线段 6', note: '中枢 2 | 趋势上' },
];

const watchlist = [
  { pair: 'ETH/USDT', last: '3,492.1', signal: 'buy', signalLabel: '买', bias: '+1.2%' },
  { pair: 'SOL/USDT', last: '142.8', signal: 'wait', signalLabel: '观望', bias: '+0.3%' },
  { pair: 'BNB/USDT', last: '612.4', signal: 'sell', signalLabel: '卖', bias: '-0.9%' },
  { pair: 'ARB/USDT', last: '1.16', signal: 'wait', signalLabel: '观望', bias: '+0.1%' },
];
</script>
