<template>
  <section class="flex flex-col gap-4">
    <CardHeader title="策略实验室" subtitle="分级参数化缠论规则">
      <template #actions>
        <div class="strategy-actions flex items-center gap-3">
          <button class="button-ghost">保存模板</button>
          <button class="button">运行回测</button>
        </div>
      </template>
    </CardHeader>

    <template v-if="loading">
      <Skeleton variant="card" height="180px" />
    </template>

    <CardSection v-else title="参数调优" layout="grid-3" gap="md">
      <div v-for="param in strategyParams" :key="param.label" class="param p-4">
        <label>{{ param.label }}</label>
        <div class="value">{{ param.value }}</div>
        <input type="range" :min="param.min" :max="param.max" :value="param.value" />
      </div>
    </CardSection>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../../components/common/CardHeader.vue';
import CardSection from '../../components/common/CardSection.vue';
import Skeleton from '../../components/common/Skeleton.vue';

const loading = ref(true);

const strategyParams = [
  { label: '最小成笔K数', value: 5, min: 3, max: 9 },
  { label: '中枢重叠度', value: 62, min: 40, max: 80 },
  { label: '背驰阈值', value: 12, min: 5, max: 25 },
];

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 1100));
  loading.value = false;
});
</script>
