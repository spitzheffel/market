<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('settings.title')" :subtitle="t('settings.subtitle')">
      <template #actions>
        <button class="button" @click="saveSettings" :disabled="saving">
          {{ saving ? t('settings.saving') : t('settings.saveSettings') }}
        </button>
      </template>
    </CardHeader>

    <div v-if="loading" class="grid gap-4 md:grid-cols-2">
      <Skeleton v-for="i in 6" :key="i" variant="card" height="120px" />
    </div>

    <div v-else class="grid gap-4 md:grid-cols-2">
      <!-- 语言设置 -->
      <CardSection :title="t('settings.language')" class="mini-card p-4">
        <LanguageSwitcher />
      </CardSection>

      <!-- 主题设置 -->
      <CardSection :title="t('settings.theme')" class="mini-card p-4">
        <div class="flex gap-2">
          <button
            :class="['theme-button', { active: theme === 'dark' }]"
            @click="theme = 'dark'"
          >
            {{ t('settings.darkMode') }}
          </button>
          <button
            :class="['theme-button', { active: theme === 'light' }]"
            @click="theme = 'light'"
          >
            {{ t('settings.lightMode') }}
          </button>
        </div>
      </CardSection>

      <!-- 风控设置 -->
      <CardSection :title="t('settings.riskControl')" class="mini-card p-4">
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <label class="text-sm text-muted">{{ t('settings.drawdownLimit') }}</label>
            <input
              v-model="riskSettings.drawdownLimit"
              type="number"
              min="1"
              max="20"
              step="0.5"
              class="setting-input w-20 text-right"
            />
            <span class="text-sm ml-1">%</span>
          </div>
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              v-model="riskSettings.autoReduceWeight"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('settings.autoReduceWeight') }}</span>
          </label>
        </div>
      </CardSection>

      <!-- 账户绑定 -->
      <CardSection :title="t('settings.accountBinding')" class="mini-card p-4">
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-sm">{{ t('settings.exchange') }}</span>
            <span class="mono text-sm">Binance</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm">{{ t('settings.status') }}</span>
            <span class="text-sm text-success">{{ t('settings.connected') }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-sm">{{ t('settings.apiKey') }}</span>
            <span class="mono text-xs text-muted">****ABCD</span>
          </div>
          <button class="button-ghost w-full mt-2">{{ t('settings.reconnect') }}</button>
        </div>
      </CardSection>

      <!-- 通知设置 -->
      <CardSection :title="t('settings.notifications')" class="mini-card p-4">
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              v-model="notificationSettings.email"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('settings.email') }}</span>
          </label>
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              v-model="notificationSettings.webhook"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('settings.webhook') }}</span>
          </label>
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              v-model="notificationSettings.telegram"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('settings.telegram') }}</span>
          </label>
        </div>
      </CardSection>

      <!-- 数据同步 -->
      <CardSection :title="t('settings.dataSync')" class="mini-card p-4">
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <label class="text-sm text-muted">{{ t('settings.syncInterval') }}</label>
            <select v-model="dataSettings.syncInterval" class="setting-input">
              <option value="1m">1m</option>
              <option value="5m">5m</option>
              <option value="15m">15m</option>
            </select>
          </div>
          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              v-model="dataSettings.autoSync"
              class="w-4 h-4 accent-[var(--accent)]"
            />
            <span>{{ t('settings.autoSync') }}</span>
          </label>
        </div>
      </CardSection>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import Skeleton from '../components/common/Skeleton.vue';
import LanguageSwitcher from '../components/common/LanguageSwitcher.vue';

const { t } = useI18n();

const loading = ref(true);
const saving = ref(false);
const theme = ref('dark');

const riskSettings = reactive({
  drawdownLimit: 3,
  autoReduceWeight: true
});

const notificationSettings = reactive({
  email: true,
  webhook: false,
  telegram: false
});

const dataSettings = reactive({
  syncInterval: '1m',
  autoSync: true
});

const saveSettings = async () => {
  saving.value = true;
  await new Promise((resolve) => setTimeout(resolve, 1000));
  saving.value = false;
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 600));
  loading.value = false;
});
</script>

<style scoped>
.theme-button {
  flex: 1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s;
}

.theme-button:hover {
  background: rgba(148, 163, 184, 0.15);
  border-color: rgba(148, 163, 184, 0.3);
}

.theme-button.active {
  background: rgba(59, 130, 246, 0.15);
  border-color: var(--accent);
  color: var(--accent);
}

.setting-input {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 14px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: var(--ink);
  outline: none;
  transition: all 0.2s;
}

.setting-input:hover {
  border-color: rgba(148, 163, 184, 0.4);
}

.setting-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.text-success {
  color: #22c55e;
}
</style>
