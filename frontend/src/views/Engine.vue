<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('engine.title')" :subtitle="t('engine.subtitle')">
      <template #actions>
        <div class="flex gap-2">
          <button class="button-ghost" @click="resetParams">{{ t('engine.reset') }}</button>
          <button class="button" :disabled="saving" @click="saveParams">
            {{ saving ? t('engine.saving') : t('engine.save') }}
          </button>
        </div>
      </template>
    </CardHeader>

    <template v-if="loading">
      <Skeleton variant="card" height="200px" />
      <Skeleton variant="card" height="100px" />
    </template>

    <template v-else>
      <!-- 参数配置 -->
      <CardSection :title="t('engine.parameters')" :subtitle="t('engine.parametersDesc')" layout="grid-3" gap="md">
        <div v-for="param in engineParams" :key="param.key" class="param p-4">
          <div class="flex items-center justify-between mb-2">
            <label>{{ t(param.labelKey) }}</label>
            <span class="mono text-sm text-accent">{{ param.value }}</span>
          </div>
          <input
            type="range"
            :min="param.min"
            :max="param.max"
            v-model.number="param.value"
            class="w-full"
          />
          <div class="flex justify-between text-xs text-muted mt-1">
            <span>{{ param.min }}</span>
            <span>{{ param.max }}</span>
          </div>
        </div>
      </CardSection>

      <!-- 计算结果 -->
      <CardSection :title="t('engine.results')" :subtitle="t('engine.resultsDesc')" bordered>
        <!-- 统计概览 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
          <div class="mini-card p-3">
            <div class="text-xs text-muted">{{ t('engine.totalFenxing') }}</div>
            <div class="mono text-lg font-semibold mt-1">{{ stats.totalFenxing }}</div>
            <div class="text-xs text-muted mt-1">
              {{ t('engine.top') }}: {{ stats.topFenxing }} / {{ t('engine.bottom') }}: {{ stats.bottomFenxing }}
            </div>
          </div>
          <div class="mini-card p-3">
            <div class="text-xs text-muted">{{ t('engine.totalBi') }}</div>
            <div class="mono text-lg font-semibold mt-1">{{ stats.totalBi }}</div>
            <div class="text-xs text-muted mt-1">
              {{ t('engine.up') }}: {{ stats.upBi }} / {{ t('engine.down') }}: {{ stats.downBi }}
            </div>
          </div>
          <div class="mini-card p-3">
            <div class="text-xs text-muted">{{ t('engine.totalXianduan') }}</div>
            <div class="mono text-lg font-semibold mt-1">{{ stats.totalXianduan }}</div>
            <div class="text-xs text-muted mt-1">
              {{ t('engine.up') }}: {{ stats.upXianduan }} / {{ t('engine.down') }}: {{ stats.downXianduan }}
            </div>
          </div>
          <div class="mini-card p-3">
            <div class="text-xs text-muted">{{ t('engine.totalZhongshu') }}</div>
            <div class="mono text-lg font-semibold mt-1">{{ stats.totalZhongshu }}</div>
            <div class="text-xs text-muted mt-1">
              {{ t('engine.tradingPoints') }}: {{ stats.totalTradingPoints }}
            </div>
          </div>
        </div>

        <!-- 当前结构 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <!-- 当前笔 -->
          <div class="mini-card p-4">
            <div class="flex items-center justify-between mb-3">
              <div class="text-sm font-medium">{{ t('engine.currentBi') }}</div>
              <span :class="['direction-badge', currentBi.direction]">
                {{ currentBi.direction === 'up' ? '↑' : '↓' }}
              </span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.range') }}</span>
                <span class="mono">${{ currentBi.startPrice }} → ${{ currentBi.endPrice }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.klineCount') }}</span>
                <span class="mono">{{ currentBi.klineCount }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.strength') }}</span>
                <span :class="['strength-badge', currentBi.strength]">
                  {{ formatStrength(currentBi.strength) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 当前线段 -->
          <div class="mini-card p-4">
            <div class="flex items-center justify-between mb-3">
              <div class="text-sm font-medium">{{ t('engine.currentXianduan') }}</div>
              <span :class="['direction-badge', currentXianduan.direction]">
                {{ currentXianduan.direction === 'up' ? '↑' : '↓' }}
              </span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.range') }}</span>
                <span class="mono">${{ currentXianduan.startPrice }} → ${{ currentXianduan.endPrice }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.biCount') }}</span>
                <span class="mono">{{ currentXianduan.biCount }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.strength') }}</span>
                <span :class="['strength-badge', currentXianduan.strength]">
                  {{ formatStrength(currentXianduan.strength) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 当前中枢 -->
          <div class="mini-card p-4">
            <div class="flex items-center justify-between mb-3">
              <div class="text-sm font-medium">{{ t('engine.currentZhongshu') }}</div>
              <span class="level-badge">{{ currentZhongshu.level }}</span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.range') }}</span>
                <span class="mono">${{ currentZhongshu.low }} - ${{ currentZhongshu.high }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.center') }}</span>
                <span class="mono">${{ currentZhongshu.center }}</span>
              </div>
              <div class="flex justify-between text-xs">
                <span class="text-muted">{{ t('engine.oscillations') }}</span>
                <span class="mono">{{ currentZhongshu.oscillations }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近买卖点 -->
        <div class="mini-card p-4 mt-3">
          <div class="text-sm font-medium mb-3">{{ t('engine.recentTradingPoints') }}</div>
          <div class="space-y-2">
            <div
              v-for="point in recentTradingPoints"
              :key="point.index"
              class="flex items-center justify-between p-2 rounded-lg bg-[rgba(148,163,184,0.05)]"
            >
              <div class="flex items-center gap-3">
                <span :class="['point-badge', point.type]">
                  {{ point.type === 'buy' ? 'B' : 'S' }}{{ point.level }}
                </span>
                <div>
                  <div class="text-sm">{{ point.reason }}</div>
                  <div class="text-xs text-muted">{{ formatTime(point.time) }}</div>
                </div>
              </div>
              <div class="text-right">
                <div class="mono text-sm">${{ point.price.toLocaleString() }}</div>
                <div :class="['text-xs', `confidence-${point.confidence}`]">
                  {{ formatConfidence(point.confidence) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </CardSection>

      <!-- 说明 -->
      <CardSection :title="t('engine.info')" bordered>
        <div class="mini-card p-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-lg bg-[rgba(59,130,246,0.15)] flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-accent" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 16v-4M12 8h.01" />
              </svg>
            </div>
            <div>
              <div class="text-sm font-medium mb-1">{{ t('engine.infoTitle') }}</div>
              <div class="text-muted text-sm leading-relaxed">
                {{ t('engine.infoDesc') }}
              </div>
            </div>
          </div>
        </div>
      </CardSection>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import Skeleton from '../components/common/Skeleton.vue';
import {
  mockCurrentBi,
  mockCurrentXianduan,
  mockCurrentZhongshu,
  mockRecentTradingPoints,
  mockEngineStats
} from '../mock/engineData';

const { t, locale } = useI18n();
const loading = ref(true);
const saving = ref(false);

const defaultParams = [
  { key: 'mergeRelation', labelKey: 'engine.mergeRelation', value: 1, min: 0, max: 2 },
  { key: 'minFenxingK', labelKey: 'engine.minFenxingK', value: 3, min: 2, max: 6 },
  { key: 'minXianduanBi', labelKey: 'engine.minXianduanBi', value: 3, min: 3, max: 6 },
];

const engineParams = ref(defaultParams.map((p) => ({ ...p })));
const currentBi = ref(mockCurrentBi);
const currentXianduan = ref(mockCurrentXianduan);
const currentZhongshu = ref(mockCurrentZhongshu);
const recentTradingPoints = ref(mockRecentTradingPoints);
const stats = ref(mockEngineStats);

const saveParams = async () => {
  saving.value = true;
  await new Promise((resolve) => setTimeout(resolve, 1000));
  saving.value = false;
};

const resetParams = () => {
  engineParams.value = defaultParams.map((p) => ({ ...p }));
};

const formatStrength = (strength) => {
  const map = {
    strong: t('engine.strong'),
    medium: t('engine.medium'),
    weak: t('engine.weak')
  };
  return map[strength] || strength;
};

const formatConfidence = (confidence) => {
  const map = {
    high: t('engine.high'),
    medium: t('engine.medium'),
    low: t('engine.low')
  };
  return map[confidence] || confidence;
};

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const diffMs = Date.now() - date.getTime();
  const minutes = Math.floor(diffMs / 60000);
  const hours = Math.floor(diffMs / 3600000);
  const days = Math.floor(diffMs / 86400000);

  if (minutes < 60) {
    const rtf = new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' });
    return rtf.format(-Math.max(0, minutes), 'minute');
  }

  if (hours < 24) {
    const rtf = new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' });
    return rtf.format(-hours, 'hour');
  }

  if (days < 7) {
    const rtf = new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' });
    return rtf.format(-days, 'day');
  }

  return new Intl.DateTimeFormat(locale.value, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 700));
  loading.value = false;
});
</script>

<style scoped>
.text-accent {
  color: var(--accent);
}

.direction-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
}

.direction-badge.up {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.direction-badge.down {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.strength-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.strength-badge.strong {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.strength-badge.medium {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.strength-badge.weak {
  background: rgba(148, 163, 184, 0.15);
  color: var(--muted);
}

.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.point-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: bold;
}

.point-badge.buy {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.point-badge.sell {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.confidence-high {
  color: #22c55e;
}

.confidence-medium {
  color: #fbbf24;
}

.confidence-low {
  color: var(--muted);
}
</style>
