<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('backtest.title')" :subtitle="t('backtest.subtitle')">
      <template #actions>
        <button class="button">{{ t('backtest.newBacktest') }}</button>
      </template>
    </CardHeader>

    <!-- Loading state -->
    <div v-if="loading" class="grid gap-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="100px" />
    </div>

    <!-- Empty state -->
    <EmptyState
      v-else-if="!backtests.length"
      :title="t('backtest.emptyTitle')"
      :description="t('backtest.emptyDesc')"
    >
      <template #action>
        <button class="button">{{ t('backtest.newBacktest') }}</button>
      </template>
    </EmptyState>

    <!-- Backtest list -->
    <div v-else class="grid gap-3">
      <div v-for="task in backtests" :key="task.id" class="mini-card p-4">
        <div class="flex justify-between items-start mb-2">
          <div class="mono font-semibold">{{ task.name }}</div>
          <div class="flex items-center gap-2">
            <StatusTag
              :variant="getStatusVariant(task.status)"
              :label="task.statusLabel"
              :dot="task.status === 'running'"
            />
            <button
              class="button-ghost text-xs py-1.5 px-3"
              @click="selectedTaskId = task.id"
            >
              {{ t('backtest.viewReport') }}
            </button>
          </div>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('backtest.range') }}</div>
            <div class="mono text-sm mt-1">{{ task.range }}</div>
          </div>
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('backtest.pnl') }}</div>
            <div
              class="mono text-sm mt-1"
              :class="task.pnl.startsWith('+') ? 'text-success' : task.pnl.startsWith('-') ? 'text-danger' : 'text-muted'"
            >
              {{ task.pnl }}
            </div>
          </div>
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('backtest.drawdown') }}</div>
            <div class="mono text-sm mt-1">{{ task.dd }}</div>
          </div>
          <div v-if="task.status === 'running'">
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('backtest.progress') }}</div>
            <div class="mt-2">
              <div class="h-1.5 rounded-full bg-[rgba(148,163,184,0.18)] overflow-hidden">
                <div
                  class="h-full bg-gradient-to-r from-[#38bdf8] to-[#22d3ee] transition-all"
                  :style="{ width: task.progress || '45%' }"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <CardSection :title="t('backtest.report')" :subtitle="t('backtest.reportDesc')" bordered>
      <div class="text-xs text-muted mb-3">
        {{ t('backtest.reportFor') }}
        <span class="mono">{{ activeTask?.name || '--' }}</span>
      </div>
      <div class="grid gap-3 md:grid-cols-3">
        <KpiStat
          v-for="stat in reportStats"
          :key="stat.labelKey"
          :label="t(stat.labelKey)"
          :value="stat.value"
          :tone="stat.tone"
        />
      </div>
    </CardSection>

    <CardSection :title="t('backtest.equityCurve')" :subtitle="t('backtest.equityCurveDesc')" bordered>
      <div class="mini-card p-4">
        <div class="h-32 rounded-lg border border-[rgba(148,163,184,0.2)] bg-[rgba(15,23,42,0.6)] flex items-center justify-center">
          <svg viewBox="0 0 600 160" class="w-full h-full">
            <defs>
              <linearGradient id="equityGradient" x1="0" x2="1" y1="0" y2="0">
                <stop offset="0%" stop-color="#38bdf8" stop-opacity="0.8" />
                <stop offset="100%" stop-color="#22d3ee" stop-opacity="0.8" />
              </linearGradient>
            </defs>
            <path
              d="M10 120 L80 110 L150 90 L220 95 L300 70 L360 60 L430 65 L520 40 L590 35"
              fill="none"
              stroke="url(#equityGradient)"
              stroke-width="3"
            />
          </svg>
        </div>
      </div>
    </CardSection>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import StatusTag from '../components/common/StatusTag.vue';
import KpiStat from '../components/common/KpiStat.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';

const { t } = useI18n();

const loading = ref(true);

const backtestsRaw = ref([
  { id: 1, nameKey: 'backtest.taskNames.btc1mThirdBuy', status: 'done', range: '2024-01 ~ 2024-03', pnl: '+12.4%', dd: '3.1%' },
  { id: 2, nameKey: 'backtest.taskNames.eth5mBreakout', status: 'running', range: '2024-02 ~ 2024-03', pnl: '+5.2%', dd: '2.4%', progress: '65%' },
  { id: 3, nameKey: 'backtest.taskNames.sol15mBacktest', status: 'queued', range: '2023-12 ~ 2024-03', pnl: '--', dd: '--' },
]);

const selectedTaskId = ref(backtestsRaw.value[0]?.id || null);

const backtests = computed(() =>
  backtestsRaw.value.map(task => ({
    ...task,
    name: t(task.nameKey),
    statusLabel: t(`backtest.${task.status}`),
  }))
);

const activeTask = computed(() => {
  return backtests.value.find((task) => task.id === selectedTaskId.value) || backtests.value[0];
});

const reportStats = computed(() => ([
  {
    labelKey: 'backtest.roi',
    value: activeTask.value?.pnl || '--',
    tone: activeTask.value?.pnl?.startsWith('+') ? 'up' : activeTask.value?.pnl?.startsWith('-') ? 'down' : 'default',
  },
  { labelKey: 'backtest.winRate', value: '58%', tone: 'default' },
  { labelKey: 'backtest.profitFactor', value: '1.42', tone: 'default' },
  { labelKey: 'backtest.maxDrawdown', value: activeTask.value?.dd || '--', tone: 'warn' },
  { labelKey: 'backtest.tradeCount', value: '128', tone: 'default' },
  { labelKey: 'backtest.avgHold', value: '3.2h', tone: 'default' },
]));

const getStatusVariant = (status) => {
  const map = {
    done: 'success',
    running: 'running',
    queued: 'queued',
    failed: 'danger',
  };
  return map[status] || 'neutral';
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 900));
  loading.value = false;
});
</script>
