<template>
  <!--
    ⚠️ PLACEHOLDER/EXAMPLE CODE - NOT PART OF PHASE 2 ⚠️

    This page demonstrates UI patterns for signal display but does NOT implement
    real Chan Theory signal generation. Current implementation uses price change
    percentages as mock signals.

    Phase 2 Scope: Chan calculation + chart visualization only
    Phase 3 Scope: Real signal generation, push notifications, alert system

    TODO Phase 3:
    - Integrate with backend /api/chan/trading-points endpoint
    - Replace mock signal generation with real Chan Theory signals
    - Implement signal push/notification system
    - Add signal history and performance tracking
  -->
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('signals.title')" :subtitle="t('signals.subtitle')">
      <template #actions>
        <div class="flex flex-wrap gap-2 items-center">
          <FilterPill
            v-for="dir in directionOptions"
            :key="dir.value"
            :label="dir.label"
            :active="filters.direction === dir.value"
            @click="filters.direction = dir.value"
          />
          <button class="button" @click="openAlertModal">
            {{ t('signals.createAlert') }}
          </button>
        </div>
      </template>
    </CardHeader>

    <div class="flex flex-wrap items-center gap-3">
      <input
        v-model="searchQuery"
        type="text"
        :placeholder="t('signals.searchPlaceholder')"
        class="w-full md:w-64 rounded-full border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-4 py-2 text-sm text-slate-100 placeholder:text-slate-500"
      />
      <FilterGroup :label="t('common.level')">
        <FilterPill
          v-for="level in levelOptions"
          :key="level"
          :label="level"
          :active="filters.level === level"
          @click="filters.level = level"
        />
      </FilterGroup>
      <FilterGroup :label="t('common.confidence')">
        <FilterPill
          v-for="conf in confidenceOptions"
          :key="conf.value"
          :label="conf.label"
          :active="filters.confidence === conf.value"
          @click="filters.confidence = conf.value"
        />
      </FilterGroup>
    </div>

    <!-- Signal list with loading/empty states -->
    <div v-if="loading" class="grid gap-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="80px" />
    </div>

    <EmptyState
      v-else-if="!filteredSignals.length"
      :title="t('signals.emptyTitle')"
      :description="t('signals.emptyDesc')"
      size="sm"
    />

    <div v-else class="grid gap-3">
      <div
        v-for="signal in filteredSignals"
        :key="signal.code"
        class="signal-item grid grid-cols-[auto,1fr,auto] md:grid-cols-[auto,1fr,auto] items-center gap-3 p-3"
      >
        <StatusTag :variant="signal.tag" :label="signal.code" />
        <div>
          <div class="signal-title flex items-center gap-2 flex-wrap">
            <span>{{ signal.title }}</span>
            <StatusTag
              :variant="signal.priorityVariant"
              :label="signal.priorityLabel"
              size="sm"
            />
          </div>
          <div class="signal-meta">{{ signal.meta }}</div>
        </div>
        <div class="flex flex-col items-end gap-1 text-right">
          <div class="mono text-sm">{{ signal.time }}</div>
          <div class="text-xs text-muted hidden md:block">{{ t('common.confidence') }} {{ signal.confidence }}</div>
          <div class="text-xs text-muted hidden md:block">{{ t('common.validity') }} {{ signal.expires }}</div>
        </div>
      </div>
    </div>

    <CardSection
      class="mt-2"
      :title="t('signals.alertRules')"
      :subtitle="t('signals.alertRulesDesc')"
      layout="grid-2"
      gap="md"
    >
      <div class="mini-card p-4">
        <div class="space-y-3">
          <div
            v-for="rule in alertRules"
            :key="rule.id"
            class="flex items-start justify-between gap-3"
          >
            <div>
              <div class="text-sm font-medium">{{ rule.symbol }} | {{ rule.interval }}</div>
              <div class="text-xs text-muted">{{ t(rule.conditionKey) }}</div>
            </div>
            <div class="flex items-center gap-2">
              <StatusTag
                :variant="getPriorityVariant(rule.priority)"
                :label="t(`common.${rule.priority}`)"
                size="sm"
              />
              <label class="flex items-center gap-1 text-xs text-muted">
                <input
                  type="checkbox"
                  v-model="rule.enabled"
                  class="w-4 h-4 accent-[var(--accent)]"
                />
                <span>{{ rule.enabled ? t('signals.enabled') : t('signals.disabled') }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>
      <div class="mini-card p-4">
        <div class="text-xs text-muted uppercase tracking-wide">{{ t('signals.deliveryChannels') }}</div>
        <div class="text-xs text-muted mb-3">{{ t('signals.deliveryChannelsDesc') }}</div>
        <div class="space-y-2 text-sm">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="channelSettings.inApp"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('signals.channelInApp') }}</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="channelSettings.email"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('signals.channelEmail') }}</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="channelSettings.webhook"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('signals.channelWebhook') }}</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="channelSettings.telegram"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('signals.channelTelegram') }}</span>
          </label>
        </div>
      </div>
    </CardSection>

    <Modal v-model="showAlertModal" :title="t('signals.createAlert')" size="lg">
      <div class="grid gap-4 md:grid-cols-2">
        <div class="space-y-3">
          <div class="space-y-1">
            <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertName') }}</label>
            <input
              v-model="alertForm.name"
              type="text"
              :placeholder="t('signals.alertNamePlaceholder')"
              class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500"
            />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1">
              <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.symbol') }}</label>
              <select
                v-model="alertForm.symbol"
                class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
              >
                <option v-for="symbol in alertSymbols" :key="symbol" :value="symbol">{{ symbol }}</option>
              </select>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.interval') }}</label>
              <select
                v-model="alertForm.interval"
                class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
              >
                <option v-for="interval in levelOptions" :key="interval" :value="interval">{{ interval }}</option>
              </select>
            </div>
          </div>
          <div class="space-y-1">
            <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertCondition') }}</label>
            <select
              v-model="alertForm.condition"
              class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
            >
              <option v-for="opt in conditionOptions" :key="opt.value" :value="opt.value">
                {{ t(opt.labelKey) }}
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1">
              <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertPriority') }}</label>
              <select
                v-model="alertForm.priority"
                class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
              >
                <option v-for="prio in priorityOptions" :key="prio" :value="prio">{{ t(`common.${prio}`) }}</option>
              </select>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertExpiry') }}</label>
              <select
                v-model="alertForm.expiry"
                class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
              >
                <option v-for="exp in expiryOptions" :key="exp.value" :value="exp.value">{{ exp.label }}</option>
              </select>
            </div>
          </div>
        </div>
        <div class="space-y-3">
          <div class="mini-card p-4 space-y-2">
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertChannels') }}</div>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="alertChannels.inApp" class="w-4 h-4 accent-[var(--accent)]" />
              <span>{{ t('signals.channelInApp') }}</span>
            </label>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="alertChannels.email" class="w-4 h-4 accent-[var(--accent)]" />
              <span>{{ t('signals.channelEmail') }}</span>
            </label>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="alertChannels.webhook" class="w-4 h-4 accent-[var(--accent)]" />
              <span>{{ t('signals.channelWebhook') }}</span>
            </label>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="alertChannels.telegram" class="w-4 h-4 accent-[var(--accent)]" />
              <span>{{ t('signals.channelTelegram') }}</span>
            </label>
          </div>
          <div class="mini-card p-4 space-y-2">
            <div class="text-xs text-muted uppercase tracking-wide">{{ t('signals.alertPreview') }}</div>
            <div class="text-sm font-medium">
              {{ alertForm.symbol }} | {{ alertForm.interval }}
            </div>
            <div class="text-xs text-muted">{{ t(conditionKeyMap[alertForm.condition]) }}</div>
            <div class="text-xs text-muted">
              {{ t('signals.alertPriority') }}: {{ t(`common.${alertForm.priority}`) }} · {{ t('signals.alertExpiry') }}: {{ alertForm.expiry }}
            </div>
            <div class="text-xs text-muted">
              {{ t('signals.alertChannels') }}: {{ selectedChannelLabels.join(' / ') || '--' }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <button class="button-ghost" @click="closeAlertModal">{{ t('common.cancel') }}</button>
        <button class="button" @click="saveAlert">{{ t('signals.saveAlert') }}</button>
      </template>
    </Modal>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';
import CardSection from '../components/common/CardSection.vue';
import Modal from '../components/common/Modal.vue';
import { klineApi, marketCatalogApi } from '../api/market';

const { t } = useI18n();

const loading = ref(false);
const searchQuery = ref('');

const filters = reactive({
  direction: 'all',
  level: '1m',
  confidence: 'all',
});

const directionOptions = computed(() => [
  { label: t('common.all'), value: 'all' },
  { label: t('signals.buy'), value: 'buy' },
  { label: t('signals.sell'), value: 'sell' },
]);

const levelOptions = ['1m', '5m', '15m'];
const alertSymbols = ref([]);

const confidenceOptions = computed(() => [
  { label: '>0.7', value: '0.7' },
  { label: '>0.5', value: '0.5' },
  { label: t('common.all'), value: 'all' },
]);

const signalFeed = ref([]);

const fallbackSymbols = ['BTC/USDT', 'ETH/USDT', 'SOL/USDT', 'BNB/USDT', 'ADA/USDT'];
const signalMetaKeys = [
  'signals.meta.hubBreakout',
  'signals.meta.weakDivergence',
  'signals.meta.segmentExhaustion',
  'signals.meta.strokeBreakout',
  'signals.meta.divergenceSellPoint',
];

const formatTime = (date) => {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
};

const getSignalTag = (changePercent) => {
  if (changePercent >= 0.8) return 'buy';
  if (changePercent <= -0.8) return 'sell';
  return 'wait';
};

const getPriority = (absChange) => {
  if (absChange >= 2) return 'high';
  if (absChange >= 1) return 'medium';
  return 'low';
};

const getExpires = (level) => {
  const map = { '1m': '3m', '5m': '15m', '15m': '45m' };
  return map[level] || '15m';
};

const buildSignal = (ticker, index) => {
  const level = levelOptions[index % levelOptions.length];
  const changePercent = Number.parseFloat(ticker.priceChangePercent || '0');
  const absChange = Number.isNaN(changePercent) ? 0 : Math.abs(changePercent);
  const safeChange = Number.isNaN(changePercent) ? 0 : changePercent;
  const tag = getSignalTag(safeChange);
  const confidence = Math.min(0.9, 0.55 + absChange / 10).toFixed(2);
  const priority = getPriority(absChange);
  const codePrefix = tag === 'buy' ? 'B' : tag === 'sell' ? 'S' : 'W';
  const metaKey = signalMetaKeys[index % signalMetaKeys.length];

  return {
    tag,
    code: `${codePrefix}${(index % 9) + 1}`,
    title: `${ticker.symbol} | ${level}`,
    metaKey,
    time: formatTime(new Date()),
    priority,
    confidence,
    expires: getExpires(level),
    level,
  };
};

const loadSignals = async () => {
  loading.value = true;
  try {
    const symbols = await marketCatalogApi.getSymbols();
    const symbolList = symbols.length ? symbols.map((item) => item.symbol) : fallbackSymbols;
    alertSymbols.value = symbolList;
    if (!alertSymbols.value.includes(alertForm.symbol)) {
      alertForm.symbol = alertSymbols.value[0] || 'BTC/USDT';
    }

    const tickers = await Promise.all(
      symbolList.slice(0, 12).map(async (symbol, index) => {
        try {
          const ticker = await klineApi.getTicker(symbol);
          return buildSignal(ticker, index);
        } catch (error) {
          console.warn('Failed to load ticker for', symbol, error);
          return null;
        }
      })
    );

    signalFeed.value = tickers.filter(Boolean);
  } catch (error) {
    console.error('Failed to load signals', error);
    alertSymbols.value = fallbackSymbols;
    signalFeed.value = [];
  } finally {
    loading.value = false;
  }
};

const showAlertModal = ref(false);

const conditionKeyMap = {
  hubBreakout: 'signals.ruleHubBreakout',
  weakDivergence: 'signals.ruleWeakDivergence',
  trendShift: 'signals.ruleTrendShift',
};

const conditionOptions = [
  { value: 'hubBreakout', labelKey: 'signals.ruleHubBreakout' },
  { value: 'weakDivergence', labelKey: 'signals.ruleWeakDivergence' },
  { value: 'trendShift', labelKey: 'signals.ruleTrendShift' },
];

const priorityOptions = ['high', 'medium', 'low'];

const expiryOptions = [
  { value: '15m', label: '15m' },
  { value: '1h', label: '1h' },
  { value: '4h', label: '4h' },
];

const alertForm = reactive({
  name: '',
  symbol: 'BTC/USDT',
  interval: '15m',
  condition: 'hubBreakout',
  priority: 'high',
  expiry: '4h',
});

const alertChannels = reactive({
  inApp: true,
  email: true,
  webhook: false,
  telegram: false,
});

const selectedChannelLabels = computed(() => {
  const labels = [];
  if (alertChannels.inApp) labels.push(t('signals.channelInApp'));
  if (alertChannels.email) labels.push(t('signals.channelEmail'));
  if (alertChannels.webhook) labels.push(t('signals.channelWebhook'));
  if (alertChannels.telegram) labels.push(t('signals.channelTelegram'));
  return labels;
});

const alertRules = ref([
  { id: 'rule-1', symbol: 'BTC/USDT', interval: '15m', conditionKey: 'signals.ruleHubBreakout', priority: 'high', enabled: true },
  { id: 'rule-2', symbol: 'ETH/USDT', interval: '5m', conditionKey: 'signals.ruleWeakDivergence', priority: 'medium', enabled: true },
  { id: 'rule-3', symbol: 'SOL/USDT', interval: '1m', conditionKey: 'signals.ruleTrendShift', priority: 'low', enabled: false },
]);

const channelSettings = reactive({
  inApp: true,
  email: true,
  webhook: false,
  telegram: false,
});

const getPriorityVariant = (priority) => {
  if (priority === 'high') return 'danger';
  if (priority === 'medium') return 'warning';
  return 'neutral';
};

const openAlertModal = () => {
  showAlertModal.value = true;
};

const closeAlertModal = () => {
  showAlertModal.value = false;
};

const resetAlertForm = () => {
  alertForm.name = '';
  alertForm.symbol = alertSymbols.value[0] || 'BTC/USDT';
  alertForm.interval = '15m';
  alertForm.condition = 'hubBreakout';
  alertForm.priority = 'high';
  alertForm.expiry = '4h';
  alertChannels.inApp = true;
  alertChannels.email = true;
  alertChannels.webhook = false;
  alertChannels.telegram = false;
};

const saveAlert = () => {
  alertRules.value.unshift({
    id: `rule-${Date.now()}`,
    symbol: alertForm.symbol,
    interval: alertForm.interval,
    conditionKey: conditionKeyMap[alertForm.condition],
    priority: alertForm.priority,
    enabled: true,
    channels: { ...alertChannels },
    name: alertForm.name,
    expiry: alertForm.expiry,
  });
  closeAlertModal();
  resetAlertForm();
};

onMounted(loadSignals);

const filteredSignals = computed(() => {
  let result = signalFeed.value.map((s) => ({
    ...s,
    meta: t(s.metaKey),
    priorityLabel: t(`common.${s.priority}`),
    priorityVariant: s.priority === 'high' ? 'danger' : s.priority === 'medium' ? 'warning' : 'neutral',
  }));

  // Direction filter
  if (filters.direction !== 'all') {
    result = result.filter((s) => s.tag === filters.direction);
  }

  // Level filter
  if (filters.level) {
    result = result.filter((s) => s.level === filters.level);
  }

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter((s) => s.title.toUpperCase().includes(query) || s.meta.toUpperCase().includes(query));
  }

  // Confidence filter
  if (filters.confidence !== 'all') {
    const minConf = parseFloat(filters.confidence);
    result = result.filter((s) => parseFloat(s.confidence) >= minConf);
  }

  return result;
});
</script>
