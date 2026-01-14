<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('autoTrade.title')" :subtitle="t('autoTrade.subtitle')">
      <template #actions>
        <div class="flex gap-2">
          <button class="button-ghost">{{ t('autoTrade.stopAll') }}</button>
          <button class="button">{{ t('autoTrade.start') }}</button>
        </div>
      </template>
    </CardHeader>

    <!-- 账户概览 -->
    <div v-if="loading" class="grid gap-3 md:grid-cols-4">
      <Skeleton variant="card" height="80px" />
      <Skeleton variant="card" height="80px" />
      <Skeleton variant="card" height="80px" />
      <Skeleton variant="card" height="80px" />
    </div>

    <div v-else class="grid gap-3 md:grid-cols-4">
      <KpiStat
        :label="t('autoTrade.totalEquity')"
        :value="formatCurrency(account.totalEquity)"
        :note="`${t('autoTrade.todayPnl')}: ${formatPnl(account.todayPnl, account.todayPnlPercent)}`"
        :tone="account.todayPnl >= 0 ? 'success' : 'danger'"
      />
      <KpiStat
        :label="t('autoTrade.availableBalance')"
        :value="formatCurrency(account.availableBalance)"
        :note="`${t('autoTrade.usedMargin')}: ${formatCurrency(account.usedMargin)}`"
      />
      <KpiStat
        :label="t('autoTrade.unrealizedPnl')"
        :value="formatPnl(account.unrealizedPnl)"
        :tone="account.unrealizedPnl >= 0 ? 'success' : 'danger'"
        :note="`${t('autoTrade.marginLevel')}: ${account.marginLevel}%`"
      />
      <KpiStat
        :label="t('autoTrade.positions')"
        :value="account.positions.toString()"
        :note="`${t('autoTrade.openOrders')}: ${account.openOrders}`"
      />
    </div>

    <div v-if="loading" class="grid gap-3 md:grid-cols-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="110px" />
    </div>

    <CardSection
      v-else
      :title="t('autoTrade.executionRules')"
      :subtitle="t('autoTrade.executionRulesDesc')"
      layout="grid-3"
      gap="md"
    >
      <div class="mini-card p-4 space-y-2">
        <div class="text-xs text-muted uppercase tracking-wide">{{ t('autoTrade.execution') }}</div>
        <div class="flex items-center justify-between text-sm">
          <span>{{ t('autoTrade.maxPositions') }}</span>
          <input
            v-model.number="executionRules.maxPositions"
            type="number"
            min="1"
            max="20"
            class="w-16 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-2 py-1 text-right text-sm text-slate-100"
          />
        </div>
        <div class="flex items-center justify-between text-sm">
          <span>{{ t('autoTrade.orderCooldown') }}</span>
          <input
            v-model.number="executionRules.orderCooldown"
            type="number"
            min="1"
            max="120"
            class="w-16 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-2 py-1 text-right text-sm text-slate-100"
          />
        </div>
        <div class="flex items-center justify-between text-sm">
          <span>{{ t('autoTrade.slippageLimit') }}</span>
          <div class="flex items-center gap-1">
            <input
              v-model.number="executionRules.slippageLimit"
              type="number"
              min="0.1"
              max="2"
              step="0.1"
              class="w-16 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-2 py-1 text-right text-sm text-slate-100"
            />
            <span class="text-xs text-muted">%</span>
          </div>
        </div>
      </div>
      <div class="mini-card p-4 space-y-2">
        <div class="text-xs text-muted uppercase tracking-wide">{{ t('autoTrade.risk') }}</div>
        <div class="flex items-center justify-between text-sm">
          <span>{{ t('autoTrade.maxExposure') }}</span>
          <div class="flex items-center gap-1">
            <input
              v-model.number="executionRules.maxExposure"
              type="number"
              min="5"
              max="100"
              step="1"
              class="w-16 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-2 py-1 text-right text-sm text-slate-100"
            />
            <span class="text-xs text-muted">%</span>
          </div>
        </div>
        <label class="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            v-model="executionRules.autoHedge"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('autoTrade.autoHedge') }}</span>
        </label>
      </div>
      <div class="mini-card p-4 space-y-2">
        <div class="text-xs text-muted uppercase tracking-wide">{{ t('autoTrade.safety') }}</div>
        <label class="flex items-center justify-between text-sm cursor-pointer">
          <span>{{ t('autoTrade.killSwitch') }}</span>
          <input
            type="checkbox"
            v-model="executionRules.killSwitch"
            class="w-4 h-4 accent-[var(--accent)]"
          />
        </label>
        <div class="text-xs text-muted">
          {{ t('autoTrade.killSwitchDesc') }}
        </div>
      </div>
    </CardSection>

    <!-- 标签页 -->
    <div class="tabs-container">
      <div class="tabs-header flex items-center gap-2 border-b border-[var(--line)]">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-button px-4 py-3 text-sm font-medium transition-colors relative', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="ml-2 text-xs opacity-60">({{ tab.count }})</span>
          <div v-if="activeTab === tab.key" class="tab-indicator"></div>
        </button>
      </div>

      <div class="tabs-content mt-4">
        <!-- 持仓 -->
        <div v-if="activeTab === 'positions'" class="tab-panel">
          <PositionList :positions="positions" />
        </div>

        <!-- 委托 -->
        <div v-if="activeTab === 'orders'" class="tab-panel">
          <OrderList :orders="orders" />
        </div>

        <!-- 成交 -->
        <div v-if="activeTab === 'trades'" class="tab-panel">
          <TradeList :trades="trades" />
        </div>

        <!-- 资金 -->
        <div v-if="activeTab === 'account'" class="tab-panel">
          <AccountSummary :account="account" :history="equityHistory" />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import KpiStat from '../components/common/KpiStat.vue';
import Skeleton from '../components/common/Skeleton.vue';
import CardSection from '../components/common/CardSection.vue';
import PositionList from '../components/trade/PositionList.vue';
import OrderList from '../components/trade/OrderList.vue';
import TradeList from '../components/trade/TradeList.vue';
import AccountSummary from '../components/trade/AccountSummary.vue';
import {
  mockPositions,
  mockOrders,
  mockTrades,
  mockAccount,
  mockEquityHistory
} from '../mock/tradeData';

const { t, locale } = useI18n();
const loading = ref(true);
const activeTab = ref('positions');

const positions = ref(mockPositions);
const orders = ref(mockOrders);
const trades = ref(mockTrades);
const account = ref(mockAccount);
const equityHistory = ref(mockEquityHistory);

const executionRules = reactive({
  maxPositions: 6,
  orderCooldown: 12,
  slippageLimit: 0.3,
  maxExposure: 35,
  autoHedge: false,
  killSwitch: false,
});

const tabs = computed(() => [
  { key: 'positions', label: t('autoTrade.positions'), count: positions.value.length },
  { key: 'orders', label: t('autoTrade.orders'), count: orders.value.length },
  { key: 'trades', label: t('autoTrade.trades'), count: trades.value.length },
  { key: 'account', label: t('autoTrade.account') }
]);

const formatCurrency = (value) => {
  return new Intl.NumberFormat(locale.value, {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2
  }).format(value);
};

const formatPnl = (value, percent) => {
  const sign = value >= 0 ? '+' : '';
  const formatted = formatCurrency(Math.abs(value));
  if (percent !== undefined) {
    return `${sign}${formatted} (${sign}${percent.toFixed(2)}%)`;
  }
  return `${sign}${formatted}`;
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 800));
  loading.value = false;
});
</script>

<style scoped>
.tab-button {
  color: var(--muted);
  background: transparent;
  border: none;
  cursor: pointer;
  outline: none;
}

.tab-button:hover {
  color: var(--ink);
}

.tab-button.active {
  color: var(--accent);
}

.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--accent);
}

.tab-panel {
  animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
