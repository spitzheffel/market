<template>
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
import { ref, reactive, computed } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';
import CardSection from '../components/common/CardSection.vue';
import Modal from '../components/common/Modal.vue';

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
const alertSymbols = ['BTC/USDT', 'ETH/USDT', 'SOL/USDT', 'BNB/USDT'];

const confidenceOptions = computed(() => [
  { label: '>0.7', value: '0.7' },
  { label: '>0.5', value: '0.5' },
  { label: t('common.all'), value: 'all' },
]);

const signalFeed = ref([
  { tag: 'buy', code: 'B1', title: 'BTC/USDT | 1m', metaKey: 'signals.meta.hubBreakout', time: '00:48', priority: 'high', confidence: '0.71', expires: '4m', level: '1m' },
  { tag: 'wait', code: 'S2', title: 'ETH/USDT | 5m', metaKey: 'signals.meta.weakDivergence', time: '02:14', priority: 'medium', confidence: '0.56', expires: '12m', level: '5m' },
  { tag: 'sell', code: 'S1', title: 'BNB/USDT | 15m', metaKey: 'signals.meta.segmentExhaustion', time: '04:02', priority: 'high', confidence: '0.68', expires: '28m', level: '15m' },
  { tag: 'buy', code: 'B2', title: 'SOL/USDT | 1m', metaKey: 'signals.meta.strokeBreakout', time: '01:15', priority: 'medium', confidence: '0.62', expires: '3m', level: '1m' },
  { tag: 'sell', code: 'S3', title: 'ADA/USDT | 5m', metaKey: 'signals.meta.divergenceSellPoint', time: '03:20', priority: 'high', confidence: '0.75', expires: '10m', level: '5m' },
]);

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
  alertForm.symbol = 'BTC/USDT';
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

const filteredSignals = computed(() => {
  let result = signalFeed.value.map(s => ({
    ...s,
    meta: t(s.metaKey),
    priorityLabel: t(`common.${s.priority}`),
    priorityVariant: s.priority === 'high' ? 'danger' : s.priority === 'medium' ? 'warning' : 'neutral',
  }));

  // 方向过滤
  if (filters.direction !== 'all') {
    result = result.filter((s) => s.tag === filters.direction);
  }

  // 级别过滤
  if (filters.level) {
    result = result.filter((s) => s.level === filters.level);
  }

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter((s) => s.title.toUpperCase().includes(query) || s.meta.toUpperCase().includes(query));
  }

  // 置信度过滤
  if (filters.confidence !== 'all') {
    const minConf = parseFloat(filters.confidence);
    result = result.filter((s) => parseFloat(s.confidence) >= minConf);
  }

  return result;
});
</script>
