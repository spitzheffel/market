<template>
  <section class="chart-card card flex flex-col gap-4 p-5">
    <CardHeader title="结构叠加" subtitle="笔、线段、中枢、背驰映射。">
      <template #actions>
        <button class="button-ghost">叠加：开启</button>
        <button class="button-ghost">自动缩放</button>
      </template>
    </CardHeader>
    <div class="chart-shell grid grid-rows-[1fr_auto] gap-3 p-4">
      <div class="chart-grid grid grid-cols-1 lg:grid-cols-[1fr_200px] gap-4">
        <div class="chart-visual rounded-xl border border-[rgba(148,163,184,0.25)] relative overflow-hidden p-3.5" style="height: min(320px, 60vh);">
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
          <KpiStat
            v-for="stat in miniStats"
            :key="stat.label"
            :label="stat.label"
            :value="stat.value"
            :note="stat.note"
            :tone="stat.tone"
          />
        </div>
      </div>
      <div class="chart-legend grid grid-cols-2 sm:flex sm:flex-wrap gap-3 text-sm text-muted">
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
    <div class="levels-card card flex flex-col gap-4 p-5">
      <CardHeader title="多级别快照" subtitle="1m / 5m / 15m 多周期联动">
        <template #actions>
          <button class="button-ghost">锁定联动</button>
        </template>
      </CardHeader>
      <div class="level-grid grid grid-cols-1 md:grid-cols-3 gap-3">
        <KpiStat
          v-for="level in levels"
          :key="level.level"
          :label="level.level"
          :value="level.title"
          :note="level.note"
        />
      </div>
      <div class="panel-sub">共识：买方区间形成（弱）。</div>
    </div>
    <div class="watch-card card flex flex-col gap-4 p-5">
      <CardHeader title="自选池" subtitle="自动扫描缠论信号">
        <template #actions>
          <button class="button-ghost">编辑</button>
        </template>
      </CardHeader>
      <DataTable :loading="loading" :column-count="4" :empty="!watchlist.length" empty-text="暂无自选标的">
        <template #header>
          <th>交易对</th>
          <th class="numeric">最新价</th>
          <th>信号</th>
          <th class="numeric">偏向</th>
        </template>
        <template #body>
          <tr v-for="row in watchlist" :key="row.pair">
            <td>{{ row.pair }}</td>
            <td class="numeric mono">{{ row.last }}</td>
            <td><StatusTag :variant="row.signal" :label="row.signalLabel" /></td>
            <td :class="['numeric', 'mono', row.bias.startsWith('-') ? 'text-danger' : 'text-success']">{{ row.bias }}</td>
          </tr>
        </template>
      </DataTable>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../../components/common/CardHeader.vue';
import KpiStat from '../../components/common/KpiStat.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import DataTable from '../../components/common/DataTable.vue';

const loading = ref(true);

const miniStats = [
  { label: '突破力度', value: '0.78', note: '线段 3 对比 1', tone: 'up' },
  { label: '背驰指数', value: '+12.4', note: '动能减弱', tone: 'warn' },
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

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 1000));
  loading.value = false;
});
</script>
