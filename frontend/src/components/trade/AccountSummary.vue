<template>
  <div class="account-summary">
    <!-- 账户统计 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
      <div class="mini-card p-4">
        <div class="text-xs text-muted mb-1">{{ t('autoTrade.totalEquity') }}</div>
        <div class="mono text-xl font-semibold">${{ account.totalEquity.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</div>
        <div :class="['text-xs mt-1', account.totalPnl >= 0 ? 'text-success' : 'text-danger']">
          {{ account.totalPnl >= 0 ? '+' : '' }}${{ Math.abs(account.totalPnl).toFixed(2) }} ({{ account.totalPnl >= 0 ? '+' : '' }}{{ account.totalPnlPercent.toFixed(2) }}%)
        </div>
      </div>

      <div class="mini-card p-4">
        <div class="text-xs text-muted mb-1">{{ t('autoTrade.availableBalance') }}</div>
        <div class="mono text-xl font-semibold">${{ account.availableBalance.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</div>
        <div class="text-xs text-muted mt-1">
          {{ ((account.availableBalance / account.totalEquity) * 100).toFixed(1) }}% {{ t('autoTrade.available') }}
        </div>
      </div>

      <div class="mini-card p-4">
        <div class="text-xs text-muted mb-1">{{ t('autoTrade.usedMargin') }}</div>
        <div class="mono text-xl font-semibold">${{ account.usedMargin.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</div>
        <div class="text-xs text-muted mt-1">
          {{ ((account.usedMargin / account.totalEquity) * 100).toFixed(1) }}% {{ t('autoTrade.used') }}
        </div>
      </div>

      <div class="mini-card p-4">
        <div class="text-xs text-muted mb-1">{{ t('autoTrade.marginLevel') }}</div>
        <div class="mono text-xl font-semibold">{{ account.marginLevel }}%</div>
        <div class="text-xs text-muted mt-1">
          {{ account.marginLevel > 200 ? t('autoTrade.safe') : t('autoTrade.warning') }}
        </div>
      </div>
    </div>

    <!-- 资金曲线图 -->
    <div class="mini-card p-4">
      <div class="text-sm font-medium mb-3">{{ t('autoTrade.equityCurve') }}</div>
      <div class="equity-chart">
        <v-chart
          class="chart"
          :option="chartOption"
          autoresize
        />
      </div>
    </div>

    <!-- 详细信息 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
      <div class="mini-card p-4">
        <div class="text-sm font-medium mb-3">{{ t('autoTrade.todayStats') }}</div>
        <div class="space-y-2">
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.todayPnl') }}</span>
            <span :class="['mono text-sm font-medium', account.todayPnl >= 0 ? 'text-success' : 'text-danger']">
              {{ account.todayPnl >= 0 ? '+' : '' }}${{ Math.abs(account.todayPnl).toFixed(2) }}
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.todayPnlPercent') }}</span>
            <span :class="['mono text-sm font-medium', account.todayPnlPercent >= 0 ? 'text-success' : 'text-danger']">
              {{ account.todayPnlPercent >= 0 ? '+' : '' }}{{ account.todayPnlPercent.toFixed(2) }}%
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.unrealizedPnl') }}</span>
            <span :class="['mono text-sm font-medium', account.unrealizedPnl >= 0 ? 'text-success' : 'text-danger']">
              {{ account.unrealizedPnl >= 0 ? '+' : '' }}${{ Math.abs(account.unrealizedPnl).toFixed(2) }}
            </span>
          </div>
        </div>
      </div>

      <div class="mini-card p-4">
        <div class="text-sm font-medium mb-3">{{ t('autoTrade.accountInfo') }}</div>
        <div class="space-y-2">
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.positions') }}</span>
            <span class="mono text-sm">{{ account.positions }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.openOrders') }}</span>
            <span class="mono text-sm">{{ account.openOrders }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-xs text-muted">{{ t('autoTrade.totalPnl') }}</span>
            <span :class="['mono text-sm font-medium', account.totalPnl >= 0 ? 'text-success' : 'text-danger']">
              {{ account.totalPnl >= 0 ? '+' : '' }}${{ Math.abs(account.totalPnl).toFixed(2) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from '../../composables/useI18n';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent
} from 'echarts/components';
import VChart from 'vue-echarts';

use([
  CanvasRenderer,
  LineChart,
  GridComponent,
  TooltipComponent
]);

const props = defineProps({
  account: {
    type: Object,
    required: true
  },
  history: {
    type: Array,
    required: true
  }
});

const { t } = useI18n();

const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  grid: {
    left: '3%',
    right: '3%',
    top: '10%',
    bottom: '10%',
    containLabel: true
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15, 23, 42, 0.95)',
    borderColor: 'rgba(148, 163, 184, 0.3)',
    textStyle: {
      color: '#e2e8f0'
    },
    formatter: (params) => {
      const data = params[0];
      const date = new Date(data.value[0]);
      return `
        <div style="padding: 4px;">
          <div style="margin-bottom: 4px; font-weight: bold;">${date.toLocaleDateString()}</div>
          <div>Equity: $${data.value[1].toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
        </div>
      `;
    }
  },
  xAxis: {
    type: 'time',
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
    axisLabel: {
      color: 'rgba(148, 163, 184, 0.8)',
      fontSize: 11
    },
    splitLine: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
    axisLabel: {
      color: 'rgba(148, 163, 184, 0.8)',
      fontSize: 11,
      formatter: (value) => `$${(value / 1000).toFixed(0)}k`
    },
    splitLine: {
      lineStyle: { color: 'rgba(148, 163, 184, 0.1)' }
    }
  },
  series: [
    {
      type: 'line',
      data: props.history.map(item => [item.time, item.equity]),
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: '#3b82f6',
        width: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ]
        }
      }
    }
  ]
}));
</script>

<style scoped>
.text-success {
  color: #22c55e;
}

.text-danger {
  color: #ef4444;
}

.equity-chart {
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
