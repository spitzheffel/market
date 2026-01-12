<template>
  <div class="position-list">
    <DataTable
      v-if="positions.length > 0"
      :columns="columns"
      :data="positions"
      :row-key="'id'"
    >
      <template #symbol="{ row }">
        <div class="font-medium">{{ row.symbol }}</div>
      </template>

      <template #side="{ row }">
        <span :class="['side-badge', row.side]">
          {{ row.side === 'long' ? t('autoTrade.long') : t('autoTrade.short') }}
        </span>
      </template>

      <template #size="{ row }">
        <div class="mono">{{ row.size }}</div>
      </template>

      <template #entryPrice="{ row }">
        <div class="mono text-sm">${{ row.entryPrice.toLocaleString() }}</div>
      </template>

      <template #currentPrice="{ row }">
        <div class="mono text-sm">${{ row.currentPrice.toLocaleString() }}</div>
      </template>

      <template #pnl="{ row }">
        <div :class="['mono font-medium', row.pnl >= 0 ? 'text-success' : 'text-danger']">
          {{ row.pnl >= 0 ? '+' : '' }}${{ Math.abs(row.pnl).toFixed(2) }}
          <div class="text-xs opacity-80">
            {{ row.pnl >= 0 ? '+' : '' }}{{ row.pnlPercent.toFixed(2) }}%
          </div>
        </div>
      </template>

      <template #margin="{ row }">
        <div class="mono text-sm">${{ row.margin.toLocaleString() }}</div>
      </template>

      <template #openTime="{ row }">
        <div class="text-xs text-muted">{{ formatTime(row.openTime) }}</div>
      </template>

      <template #actions="{ row }">
        <div class="flex gap-2">
          <button class="action-button" @click="closePosition(row)">
            {{ t('autoTrade.close') }}
          </button>
        </div>
      </template>
    </DataTable>

    <EmptyState
      v-else
      :title="t('autoTrade.noPositions')"
      :description="t('autoTrade.noPositionsDesc')"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from '../../composables/useI18n';
import DataTable from '../common/DataTable.vue';
import EmptyState from '../common/EmptyState.vue';

const props = defineProps({
  positions: {
    type: Array,
    required: true
  }
});

const { t } = useI18n();

const columns = computed(() => [
  { key: 'symbol', label: t('autoTrade.symbol'), width: '120px' },
  { key: 'side', label: t('autoTrade.side'), width: '80px' },
  { key: 'size', label: t('autoTrade.size'), width: '100px' },
  { key: 'entryPrice', label: t('autoTrade.entryPrice'), width: '120px' },
  { key: 'currentPrice', label: t('autoTrade.currentPrice'), width: '120px' },
  { key: 'pnl', label: t('autoTrade.pnl'), width: '140px' },
  { key: 'margin', label: t('autoTrade.margin'), width: '120px' },
  { key: 'openTime', label: t('autoTrade.openTime'), width: '140px' },
  { key: 'actions', label: t('autoTrade.actions'), width: '100px' }
]);

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  const hours = Math.floor(diff / 3600000);
  const minutes = Math.floor((diff % 3600000) / 60000);

  if (hours > 24) {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  } else if (hours > 0) {
    return `${hours}h ${minutes}m ago`;
  } else {
    return `${minutes}m ago`;
  }
};

const closePosition = (position) => {
  console.log('Close position:', position);
  // TODO: Implement close position logic
};
</script>

<style scoped>
.side-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.side-badge.long {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.side-badge.short {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.text-success {
  color: #22c55e;
}

.text-danger {
  color: #ef4444;
}

.action-button {
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s;
}

.action-button:hover {
  background: rgba(148, 163, 184, 0.2);
  border-color: rgba(148, 163, 184, 0.3);
}
</style>
