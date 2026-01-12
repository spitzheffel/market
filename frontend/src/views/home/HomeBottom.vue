<template>
  <section class="flex flex-col gap-4">
    <CardHeader :title="t('home.strategyLab')" :subtitle="t('home.strategyLabDesc')">
      <template #actions>
        <div class="strategy-actions flex items-center gap-3">
          <button class="button-ghost">{{ t('home.saveTemplate') }}</button>
          <button class="button">{{ t('home.runBacktest') }}</button>
        </div>
      </template>
    </CardHeader>

    <template v-if="loading">
      <Skeleton variant="card" height="180px" />
    </template>

    <CardSection v-else :title="t('home.parameterTuning')" layout="grid-3" gap="md">
      <div v-for="param in strategyParams" :key="param.labelKey" class="param p-4">
        <label>{{ t(param.labelKey) }}</label>
        <div class="value">{{ param.value }}</div>
        <input type="range" :min="param.min" :max="param.max" :value="param.value" />
      </div>
    </CardSection>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from '../../composables/useI18n';
import CardHeader from '../../components/common/CardHeader.vue';
import CardSection from '../../components/common/CardSection.vue';
import Skeleton from '../../components/common/Skeleton.vue';

const { t } = useI18n();
const loading = ref(true);

const strategyParams = [
  { labelKey: 'home.minBiK', value: 5, min: 3, max: 9 },
  { labelKey: 'home.hubOverlapThreshold', value: 62, min: 40, max: 80 },
  { labelKey: 'home.divergenceThreshold', value: 12, min: 5, max: 25 },
];

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 1100));
  loading.value = false;
});
</script>
