<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('strategy.title')" :subtitle="t('strategy.subtitle')">
      <template #actions>
        <div class="flex gap-2">
          <button class="button-ghost">{{ t('strategy.newTemplate') }}</button>
          <button class="button">{{ t('strategy.runBacktest') }}</button>
        </div>
      </template>
    </CardHeader>

    <!-- Loading state -->
    <div v-if="loading" class="grid gap-3 md:grid-cols-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="120px" />
    </div>

    <!-- Empty state -->
    <EmptyState
      v-else-if="!strategyTemplates.length"
      :title="t('strategy.emptyTitle')"
      :description="t('strategy.emptyDesc')"
    >
      <template #action>
        <button class="button">{{ t('strategy.newTemplate') }}</button>
      </template>
    </EmptyState>

    <!-- Strategy templates -->
    <CardSection v-else :title="t('strategy.templates')" layout="grid-3" gap="md">
      <div v-for="tpl in strategyTemplates" :key="tpl.id" class="mini-card p-4 flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <span class="text-xs text-muted uppercase tracking-wide">{{ tpl.levels }}</span>
          <StatusTag
            :variant="getRatingVariant(tpl.rating)"
            :label="tpl.rating"
            size="sm"
          />
        </div>
        <strong class="mono text-base">{{ t(tpl.nameKey) }}</strong>
        <div class="flex items-center gap-4 mt-1">
          <div class="flex flex-col">
            <span class="text-xs text-muted">{{ t('strategy.winRate') }}</span>
            <span class="mono text-sm" :class="parseFloat(tpl.winRate) >= 55 ? 'text-success' : ''">{{ tpl.winRate }}</span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs text-muted">{{ t('strategy.rating') }}</span>
            <span class="mono text-sm">{{ tpl.rating }}</span>
          </div>
        </div>
        <div class="flex gap-2 mt-2">
          <button class="button-ghost text-xs py-1.5 px-3">{{ t('strategy.edit') }}</button>
          <button class="button text-xs py-1.5 px-3">{{ t('strategy.backtest') }}</button>
        </div>
      </div>
    </CardSection>

    <CardSection
      :title="t('strategy.builder')"
      :subtitle="t('strategy.builderDesc')"
      layout="grid-2"
      gap="md"
    >
      <div class="mini-card p-4 space-y-3">
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.name') }}</label>
          <input
            v-model="builder.name"
            type="text"
            :placeholder="t('strategy.name')"
            class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500"
          />
        </div>
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.primaryInterval') }}</label>
          <select
            v-model="builder.interval"
            class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
          >
            <option v-for="int in builderIntervals" :key="int" :value="int">{{ int }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.entrySignal') }}</label>
          <select
            v-model="builder.entrySignal"
            class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
          >
            <option value="breakout">{{ t('strategy.entryBreakout') }}</option>
            <option value="pullback">{{ t('strategy.entryPullback') }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.exitRule') }}</label>
          <select
            v-model="builder.exitRule"
            class="w-full rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100"
          >
            <option value="divergence">{{ t('strategy.exitDivergence') }}</option>
            <option value="trail">{{ t('strategy.exitTrail') }}</option>
          </select>
        </div>
      </div>
      <div class="mini-card p-4 space-y-3">
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.riskBudget') }}</label>
          <div class="flex items-center gap-2">
            <input
              v-model.number="builder.riskBudget"
              type="number"
              min="0.2"
              max="5"
              step="0.1"
              class="w-24 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100 text-right"
            />
            <span class="text-sm text-muted">%</span>
          </div>
        </div>
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.maxHold') }}</label>
          <input
            v-model.number="builder.maxHold"
            type="number"
            min="1"
            max="72"
            step="1"
            class="w-28 rounded-lg border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-3 py-2 text-sm text-slate-100 text-right"
          />
        </div>
        <div class="space-y-1">
          <label class="text-xs text-muted uppercase tracking-wide">{{ t('strategy.confirmLevels') }}</label>
          <div class="flex flex-wrap gap-2">
            <label v-for="level in builderIntervals" :key="level" class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                :value="level"
                v-model="builder.confirmLevels"
                class="w-4 h-4 accent-[var(--accent)]"
              />
              <span>{{ level }}</span>
            </label>
          </div>
        </div>
        <label class="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            v-model="builder.autoOptimize"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('strategy.autoOptimize') }}</span>
        </label>
        <div class="flex gap-2 pt-2">
          <button class="button-ghost text-xs py-2 px-3">{{ t('strategy.saveDraft') }}</button>
          <button class="button text-xs py-2 px-3">{{ t('strategy.runBacktest') }}</button>
        </div>
      </div>
    </CardSection>
  </section>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';

const { t } = useI18n();

const loading = ref(true);

const strategyTemplates = ref([
  { id: 'multiLevelResonance', nameKey: 'strategy.templateNames.multiLevelResonance', levels: '1m/5m/15m', winRate: '54%', rating: 'A-' },
  { id: 'divergenceReversal', nameKey: 'strategy.templateNames.divergenceReversal', levels: '5m/15m', winRate: '51%', rating: 'B+' },
  { id: 'trendExtension', nameKey: 'strategy.templateNames.trendExtension', levels: '15m/1h', winRate: '56%', rating: 'A' },
]);

const builderIntervals = ['1m', '5m', '15m', '1h'];

const builder = reactive({
  name: '',
  interval: '5m',
  entrySignal: 'breakout',
  exitRule: 'divergence',
  riskBudget: 1.5,
  maxHold: 12,
  confirmLevels: ['1m', '5m'],
  autoOptimize: true,
});

const getRatingVariant = (rating) => {
  if (rating.startsWith('A')) return 'success';
  if (rating.startsWith('B')) return 'warning';
  return 'neutral';
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 850));
  loading.value = false;
});
</script>
