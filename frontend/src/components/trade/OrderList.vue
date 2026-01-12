<template>
  <div class="order-list">
    <DataTable
      v-if="orders.length > 0"
      :columns="columns"
      :data="orders"
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

      <template #type="{ row }">
        <span class="type-badge">
          {{ formatOrderType(row.type) }}
        </span>
      </template>

      <template #price="{ row }">
        <div class="mono text-sm">
          ${{ row.price.toLocaleString() }}
          <div v-if="row.stopPrice" class="text-xs text-muted">
            {{ t('autoTrade.stop') }}: ${{ row.stopPrice.toLocaleString() }}
          </div>
        </div>
      </template>

      <template #size="{ row }">
        <div class="mono">{{ row.size }}</div>
      </template>

      <template #filled="{ row }">
        <div class="mono text-sm">
          {{ row.filled }} / {{ row.size }}
          <div class="text-xs text-muted">
            {{ ((row.filled / row.size) * 100).toFixed(0) }}%
          </div>
        </div>
      </template>

      <template #status="{ row }">
        <span :class="['status-badge', row.status]">
          {{ formatStatus(row.status) }}
        </span>
      </template>

      <template #createTime="{ row }">
        <div class="text-xs text-muted">{{ formatTime(row.createTime) }}</div>
      </template>

      <template #actions="{ row }">
        <div class="flex gap-2">
          <button class="action-button" @click="cancelOrder(row)">
            {{ t('autoTrade.cancel') }}
          </button>
        </div>
      </template>
    </DataTable>

    <EmptyState
      v-else
      :title="t('autoTrade.noOrders')"
      :description="t('autoTrade.noOrdersDesc')"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from '../../composables/useI18n';
import DataTable from '../common/DataTable.vue';
import EmptyState from '../common/EmptyState.vue';

const props = defineProps({
  orders: {
    type: Array,
    required: true
  }
});

const { t, locale } = useI18n();

const columns = computed(() => [
  { key: 'symbol', label: t('autoTrade.symbol'), width: '120px' },
  { key: 'side', label: t('autoTrade.side'), width: '80px' },
  { key: 'type', label: t('autoTrade.type'), width: '80px' },
  { key: 'price', label: t('autoTrade.price'), width: '120px' },
  { key: 'size', label: t('autoTrade.size'), width: '100px' },
  { key: 'filled', label: t('autoTrade.filled'), width: '120px' },
  { key: 'status', label: t('autoTrade.status'), width: '100px' },
  { key: 'createTime', label: t('autoTrade.createTime'), width: '140px' },
  { key: 'actions', label: t('autoTrade.actions'), width: '100px' }
]);

const formatOrderType = (type) => {
  const types = {
    limit: t('autoTrade.limit'),
    market: t('autoTrade.market'),
    stop: t('autoTrade.stop')
  };
  return types[type] || type;
};

const formatStatus = (status) => {
  const statuses = {
    open: t('autoTrade.open'),
    pending: t('autoTrade.pending'),
    filled: t('autoTrade.filled'),
    cancelled: t('autoTrade.cancelled')
  };
  return statuses[status] || status;
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

const cancelOrder = (order) => {
  console.log('Cancel order:', order);
  // TODO: Implement cancel order logic
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

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: rgba(148, 163, 184, 0.15);
  color: var(--muted);
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge.open {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.status-badge.pending {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.status-badge.filled {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-badge.cancelled {
  background: rgba(148, 163, 184, 0.15);
  color: var(--muted);
}

.action-button {
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--ink);
  cursor: pointer;
  transition: all 0.2s;
}

.action-button:hover {
  background: rgba(148, 163, 184, 0.2);
  border-color: rgba(148, 163, 184, 0.3);
}
</style>
