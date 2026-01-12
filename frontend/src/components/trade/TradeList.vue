<template>
  <div class="trade-list">
    <DataTable
      v-if="trades.length > 0"
      :columns="columns"
      :data="trades"
      :row-key="'id'"
    >
      <template #symbol="{ row }">
        <div class="font-medium">{{ row.symbol }}</div>
      </template>

      <template #side="{ row }">
        <span :class="['side-badge', row.side]">
          {{ row.side === 'buy' ? t('autoTrade.buy') : t('autoTrade.sell') }}
        </span>
      </template>

      <template #price="{ row }">
        <div class="mono text-sm">${{ row.price.toLocaleString() }}</div>
      </template>

      <template #size="{ row }">
        <div class="mono">{{ row.size }}</div>
      </template>

      <template #total="{ row }">
        <div class="mono text-sm">${{ (row.price * row.size).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
      </template>

      <template #fee="{ row }">
        <div class="mono text-xs text-muted">
          {{ row.fee }} {{ row.feeAsset }}
        </div>
      </template>

      <template #time="{ row }">
        <div class="text-xs text-muted">{{ formatTime(row.time) }}</div>
      </template>
    </DataTable>

    <EmptyState
      v-else
      :title="t('autoTrade.noTrades')"
      :description="t('autoTrade.noTradesDesc')"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from '../../composables/useI18n';
import DataTable from '../common/DataTable.vue';
import EmptyState from '../common/EmptyState.vue';

const props = defineProps({
  trades: {
    type: Array,
    required: true
  }
});

const { t } = useI18n();

const columns = computed(() => [
  { key: 'symbol', label: t('autoTrade.symbol'), width: '120px' },
  { key: 'side', label: t('autoTrade.side'), width: '80px' },
  { key: 'price', label: t('autoTrade.price'), width: '120px' },
  { key: 'size', label: t('autoTrade.size'), width: '100px' },
  { key: 'total', label: t('autoTrade.total'), width: '140px' },
  { key: 'fee', label: t('autoTrade.fee'), width: '100px' },
  { key: 'time', label: t('autoTrade.time'), width: '140px' }
]);

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
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

.side-badge.buy {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.side-badge.sell {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}
</style>
