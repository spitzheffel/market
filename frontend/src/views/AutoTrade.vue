<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader title="自动交易" subtitle="通道状态与风控。">
      <template #actions>
        <button class="button">启动</button>
      </template>
    </CardHeader>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2">
      <Skeleton variant="card" height="80px" />
      <Skeleton variant="card" height="80px" />
    </div>

    <div v-else class="grid gap-3 md:grid-cols-2">
      <KpiStat label="交易通道" value="Binance" note="状态：在线" />
      <KpiStat label="风控阈值" value="3.0%" note="单标的回撤" tone="warn" />
    </div>

    <div class="panel-sub">最近委托：暂无</div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../components/common/CardHeader.vue';
import KpiStat from '../components/common/KpiStat.vue';
import Skeleton from '../components/common/Skeleton.vue';

const loading = ref(true);

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 800));
  loading.value = false;
});
</script>
