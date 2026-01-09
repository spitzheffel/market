<template>
  <div class="risk-bar">
    <div class="risk-bar__track">
      <div
        class="risk-bar__fill"
        :class="[`risk-bar__fill--${level}`]"
        :style="{ width: `${clampedValue}%` }"
      />
    </div>
    <div v-if="showLabel" class="risk-bar__label">
      <span class="risk-bar__value" :class="[`risk-bar__value--${level}`]">{{ clampedValue }}%</span>
      <span v-if="labelText" class="risk-bar__text">{{ labelText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  value: { type: Number, default: 0 },
  showLabel: { type: Boolean, default: false },
  labelText: { type: String, default: '' },
  /**
   * Thresholds for color levels:
   * - safe: 0-50 (green)
   * - warning: 50-75 (yellow)
   * - danger: 75-100 (red)
   */
  safeThreshold: { type: Number, default: 50 },
  warningThreshold: { type: Number, default: 75 },
});

const clampedValue = computed(() => Math.max(0, Math.min(100, props.value)));

const level = computed(() => {
  if (clampedValue.value <= props.safeThreshold) return 'safe';
  if (clampedValue.value <= props.warningThreshold) return 'warning';
  return 'danger';
});
</script>

<style scoped>
.risk-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.risk-bar__track {
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
}

.risk-bar__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease, background 0.3s ease;
}

/* Safe level - cyan/blue gradient */
.risk-bar__fill--safe {
  background: linear-gradient(90deg, #22d3ee, #38bdf8);
}

/* Warning level - yellow/orange gradient */
.risk-bar__fill--warning {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

/* Danger level - orange/red gradient */
.risk-bar__fill--danger {
  background: linear-gradient(90deg, #f97316, #ef4444);
}

.risk-bar__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
}

.risk-bar__value {
  font-family: var(--font-mono);
  font-weight: 600;
}

.risk-bar__value--safe {
  color: #22d3ee;
}

.risk-bar__value--warning {
  color: #fbbf24;
}

.risk-bar__value--danger {
  color: #ef4444;
}

.risk-bar__text {
  color: var(--muted);
}
</style>
