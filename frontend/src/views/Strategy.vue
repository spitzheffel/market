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
      <div v-for="tpl in strategyTemplates" :key="tpl.name" class="mini-card p-4 flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <span class="text-xs text-muted uppercase tracking-wide">{{ tpl.levels }}</span>
          <StatusTag
            :variant="getRatingVariant(tpl.rating)"
            :label="tpl.rating"
            size="sm"
          />
        </div>
        <strong class="mono text-base">{{ tpl.name }}</strong>
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
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';

const { t } = useI18n();

const loading = ref(true);

const strategyTemplates = ref([
  { name: '多级别共振', levels: '1m/5m/15m', winRate: '54%', rating: 'A-' },
  { name: '背驰反转', levels: '5m/15m', winRate: '51%', rating: 'B+' },
  { name: '趋势延伸', levels: '15m/1h', winRate: '56%', rating: 'A' },
]);

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
