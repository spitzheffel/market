<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('settings.title')" :subtitle="t('settings.subtitle')">
      <template #actions>
        <button class="button">{{ t('settings.saveSettings') }}</button>
      </template>
    </CardHeader>

    <div v-if="loading" class="grid gap-3 md:grid-cols-2">
      <Skeleton v-for="i in 4" :key="i" variant="card" height="100px" />
    </div>

    <div v-else class="grid gap-3 md:grid-cols-2">
      <div class="mini-card p-4">
        <div class="panel-sub">{{ t('settings.theme') }}</div>
        <div class="flex gap-2 mt-2">
          <button class="button">{{ t('settings.darkMode') }}</button>
          <button class="button-ghost">{{ t('settings.lightMode') }}</button>
        </div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">{{ t('settings.riskControl') }}</div>
        <div class="mono text-lg mt-1">{{ t('settings.drawdownLimit') }} 3%</div>
        <div class="panel-sub">{{ t('settings.autoReduceWeight') }}</div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">{{ t('settings.accountBinding') }}</div>
        <div class="mono mt-1">Binance API · {{ t('settings.connected') }}</div>
        <div class="panel-sub">Key ****ABCD</div>
      </div>
      <div class="mini-card p-4">
        <div class="panel-sub">{{ t('settings.notifications') }}</div>
        <div class="flex gap-2 mt-2">
          <button class="select-pill">{{ t('settings.email') }}</button>
          <button class="select-pill">{{ t('settings.webhook') }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import Skeleton from '../components/common/Skeleton.vue';

const { t } = useI18n();

const loading = ref(true);

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 600));
  loading.value = false;
});
</script>
