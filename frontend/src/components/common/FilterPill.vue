<template>
  <button
    class="filter-pill"
    :class="{
      'filter-pill--active': active,
      'filter-pill--loading': loading,
      'filter-pill--disabled': disabled,
    }"
    :disabled="loading || disabled"
    @click="$emit('click')"
  >
    <span v-if="loading" class="filter-pill__spinner" />
    <span v-else-if="icon" class="filter-pill__icon">
      <slot name="icon" />
    </span>
    <span class="filter-pill__label">{{ label }}</span>
    <span v-if="count !== undefined" class="filter-pill__count">{{ count }}</span>
  </button>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  active: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  icon: { type: Boolean, default: false },
  count: { type: [Number, String], default: undefined },
});

defineEmits(['click']);
</script>

<style scoped>
.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 500;
  color: var(--ink);
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--line);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

/* Mobile: smaller padding and font size */
@media (max-width: 640px) {
  .filter-pill {
    padding: 6px 12px;
    font-size: 11px;
    gap: 4px;
  }
}

.filter-pill:hover:not(:disabled) {
  border-color: rgba(148, 163, 184, 0.5);
  background: rgba(30, 41, 59, 0.5);
}

.filter-pill:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.filter-pill--active {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.6);
  color: #e2e8f0;
  font-weight: 600;
}

.filter-pill--active:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.25);
  border-color: rgba(59, 130, 246, 0.7);
}

.filter-pill--loading {
  opacity: 0.7;
  cursor: wait;
}

.filter-pill--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.filter-pill__spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(148, 163, 184, 0.3);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: filter-pill-spin 0.8s linear infinite;
}

.filter-pill__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
}

.filter-pill__icon :deep(svg) {
  width: 100%;
  height: 100%;
  stroke: currentColor;
  stroke-width: 2;
  fill: none;
}

.filter-pill__count {
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.15);
  border-radius: 999px;
  color: var(--muted);
}

.filter-pill--active .filter-pill__count {
  background: rgba(59, 130, 246, 0.3);
  color: #e2e8f0;
}

@keyframes filter-pill-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
