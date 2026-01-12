<template>
  <section class="signal-card card flex flex-col gap-4 p-5">
    <CardHeader :title="t('home.signalMonitoring')" :subtitle="t('home.signalMonitoringDesc')" />
    <div v-if="loading" class="flex flex-col gap-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="60px" />
    </div>
    <template v-else>
      <div
        v-for="signal in signalFeed"
        :key="signal.code"
        class="signal-item grid grid-cols-[auto,1fr,auto] items-center gap-3 p-3"
      >
        <StatusTag :variant="signal.tag" :label="signal.code" />
        <div>
          <div class="signal-title">{{ signal.title }}</div>
          <div class="signal-meta">{{ signal.meta }}</div>
        </div>
        <div class="mono">{{ signal.time }}</div>
      </div>
    </template>
  </section>

  <section class="risk-card card flex flex-col gap-4 p-5">
    <CardHeader :title="t('home.riskAndPosition')" :subtitle="t('home.riskAndPositionDesc')" />
    <template v-if="loading">
      <Skeleton variant="card" height="40px" />
      <div class="grid grid-cols-2 gap-3">
        <Skeleton variant="card" height="70px" />
        <Skeleton variant="card" height="70px" />
      </div>
    </template>
    <template v-else>
      <RiskBar :value="riskData.position" show-label :label-text="t('home.position')" />
      <div class="grid grid-cols-2 gap-3">
        <KpiStat :label="t('home.maxDrawdown')" :value="riskData.maxDrawdown" tone="warn" dense />
        <KpiStat :label="t('home.holdingCount')" :value="riskData.holdingCount" :note="riskData.holdings" dense />
      </div>
    </template>
  </section>

  <section class="session-card card flex flex-col gap-4 p-5">
    <CardHeader :title="t('home.tradingCalendar')" :subtitle="t('home.tradingCalendarDesc')" />
    <div v-if="loading" class="flex flex-col gap-2">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="24px" />
    </div>
    <div v-else class="session-list flex flex-col gap-2">
      <div v-for="session in sessions" :key="session.labelKey" class="session-item flex justify-between text-sm text-muted">
        <span>{{ t(session.labelKey) }}</span>
        <span class="mono">{{ session.value }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { useI18n } from '../../composables/useI18n';
import CardHeader from '../../components/common/CardHeader.vue';
import KpiStat from '../../components/common/KpiStat.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import RiskBar from '../../components/common/RiskBar.vue';
import Skeleton from '../../components/common/Skeleton.vue';

const { t } = useI18n();
const loading = ref(true);

const signalFeed = [
  { tag: 'buy', code: 'B1', title: 'BTC/USDT | 1m', meta: t('home.hubBreakout') + ' | 0.71', time: '00:48' },
  { tag: 'wait', code: 'S2', title: 'ETH/USDT | 5m', meta: t('home.weakDivergence'), time: '02:14' },
  { tag: 'sell', code: 'S1', title: 'BNB/USDT | 15m', meta: t('home.segmentExhaustion'), time: '04:02' },
];

const riskData = reactive({
  position: 62,
  maxDrawdown: '3.4%',
  holdingCount: '3',
  holdings: 'BTC, ETH, SOL',
});

const sessions = [
  { labelKey: 'home.maintenanceWindow', value: '02:00-02:05' },
  { labelKey: 'home.fundingRate', value: '08:00' },
  { labelKey: 'home.nextScan', value: '00:00:31' },
];

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 1200));
  loading.value = false;
});
</script>
