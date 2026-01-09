<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader title="设置" subtitle="偏好、风控、账户绑定。">
      <template #actions>
        <button class="button">保存设置</button>
      </template>
    </CardHeader>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2">
      <Skeleton v-for="i in 4" :key="i" variant="card" height="100px" />
    </div>

    <div v-else class="grid gap-3 md:grid-cols-2">
      <div class="mini-card p-4">
        <div class="panel-sub">主题</div>
        <div class="flex gap-2 mt-2">
          <button class="button">暗色</button>
          <button class="button-ghost">浅色</button>
        </div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">风控</div>
        <div class="mono text-lg mt-1">单标的回撤 3%</div>
        <div class="panel-sub">超限自动降权</div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">账户绑定</div>
        <div class="mono mt-1">Binance API · 已连接</div>
        <div class="panel-sub">Key ****ABCD</div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">通知方式</div>
        <div class="flex gap-2 mt-2">
          <button class="select-pill">邮件</button>
          <button class="select-pill">Webhook</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../components/common/CardHeader.vue';
import Skeleton from '../components/common/Skeleton.vue';

const loading = ref(true);

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 600));
  loading.value = false;
});
</script>
