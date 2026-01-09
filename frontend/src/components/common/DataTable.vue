<template>
  <!-- Table view (default) -->
  <div v-if="!cardMode || !isNarrowScreen" class="data-table-wrapper" :class="{ 'data-table-wrapper--sticky': stickyHeader }" :style="maxHeight ? { maxHeight } : {}">
    <table class="data-table">
      <thead class="data-table__head">
        <tr>
          <slot name="header" />
        </tr>
      </thead>
      <tbody class="data-table__body">
        <slot name="body" />
        <tr v-if="loading" class="data-table__loading">
          <td :colspan="columnCount">
            <div class="data-table__loading-content">
              <span class="data-table__spinner" />
              <span>加载中...</span>
            </div>
          </td>
        </tr>
        <tr v-else-if="empty" class="data-table__empty">
          <td :colspan="columnCount">
            <slot name="empty">
              <EmptyState :title="emptyText" size="sm" />
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Card view (mobile) -->
  <div v-else class="data-table-cards">
    <div v-if="loading" class="data-table__loading-content">
      <span class="data-table__spinner" />
      <span>加载中...</span>
    </div>
    <div v-else-if="empty" class="data-table__empty-card">
      <slot name="empty">
        <EmptyState :title="emptyText" size="sm" />
      </slot>
    </div>
    <div v-else class="data-table-cards__list">
      <slot name="cards" :virtual="false" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import EmptyState from './EmptyState.vue';

const props = defineProps({
  stickyHeader: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  columnCount: { type: Number, default: 1 },
  maxHeight: { type: String, default: '' }, // 可选的最大高度，如 '400px'
  cardMode: { type: Boolean, default: false }, // 启用卡片模式（移动端）
});

// Detect narrow screen (<=640px)
const isNarrowScreen = ref(false);

const checkScreenSize = () => {
  isNarrowScreen.value = window.innerWidth <= 640;
};

onMounted(() => {
  if (props.cardMode) {
    checkScreenSize();
    window.addEventListener('resize', checkScreenSize);
  }
});

onUnmounted(() => {
  if (props.cardMode) {
    window.removeEventListener('resize', checkScreenSize);
  }
});
</script>

<style scoped>
.data-table-wrapper {
  overflow-x: auto;
  border-radius: var(--radius-sm);
}

.data-table-wrapper--sticky {
  overflow-y: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

/* Header */
.data-table__head {
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 10px;
  color: var(--muted);
}

.data-table-wrapper--sticky .data-table__head {
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table__head tr {
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(8px);
}

.data-table__head :deep(th) {
  padding: 10px 8px;
  text-align: left;
  font-weight: 600;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}

/* Body */
.data-table__body :deep(tr) {
  transition: background-color 0.15s ease;
}

.data-table__body :deep(tr:hover) {
  background: rgba(59, 130, 246, 0.08);
}

.data-table__body :deep(td) {
  padding: 10px 8px;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  vertical-align: middle;
}

.data-table__body :deep(tr:last-child td) {
  border-bottom: none;
}

/* Mono text */
.data-table__body :deep(td.mono) {
  font-family: var(--font-mono);
}

/* Numeric columns - right align */
.data-table__head :deep(th.text-right),
.data-table__body :deep(td.text-right) {
  text-align: right;
}

.data-table__head :deep(th.numeric),
.data-table__body :deep(td.numeric) {
  text-align: right;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

/* Loading state */
.data-table__loading td,
.data-table__empty td {
  padding: 0 !important;
  border-bottom: none !important;
}

.data-table__loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 16px;
  color: var(--muted);
  font-size: 12px;
}

.data-table__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(148, 163, 184, 0.2);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: data-table-spin 0.8s linear infinite;
}

/* Responsive: hide columns on mobile */
@media (max-width: 640px) {
  .data-table__head :deep(th.hide-mobile),
  .data-table__body :deep(td.hide-mobile) {
    display: none;
  }
}

@media (max-width: 768px) {
  .data-table__head :deep(th.hide-tablet),
  .data-table__body :deep(td.hide-tablet) {
    display: none;
  }
}

@keyframes data-table-spin {
  to {
    transform: rotate(360deg);
  }
}

/* Card mode styles */
.data-table-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.data-table-cards__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.data-table__empty-card {
  padding: 32px 16px;
}
</style>
