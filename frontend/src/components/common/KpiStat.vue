<template>
  <div class="mini-card p-3 flex flex-col" :class="[gapClass, sizeClass]">
    <span class="text-muted uppercase tracking-[0.08em]" :class="labelSizeClass">{{ label }}</span>

    <!-- Loading state -->
    <template v-if="loading">
      <div class="kpi-skeleton kpi-skeleton--value" />
      <div v-if="note" class="kpi-skeleton kpi-skeleton--note" />
    </template>

    <!-- Empty state -->
    <template v-else-if="empty">
      <strong class="mono leading-tight text-muted" :class="valueSizeClass">--</strong>
      <span v-if="resolvedEmptyText" class="panel-sub">{{ resolvedEmptyText }}</span>
    </template>

    <!-- Normal state -->
    <template v-else>
      <strong class="mono leading-tight" :class="[toneClass, valueSizeClass]">{{ value }}</strong>
      <span v-if="note" class="panel-sub">{{ note }}</span>
    </template>

    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from '../../composables/useI18n';

const { t } = useI18n();

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '' },
  note: { type: String, default: '' },
  tone: { type: String, default: 'default' }, // up | down | warn | default
  dense: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '' },
});

const resolvedEmptyText = computed(() => props.emptyText || t('common.empty'));

const toneClass = computed(() => {
  switch (props.tone) {
    case 'up':
    case 'positive':
      return 'text-success';
    case 'down':
    case 'negative':
      return 'text-danger';
    case 'warn':
    case 'warning':
      return 'text-warning';
    default:
      return '';
  }
});

const gapClass = computed(() => (props.dense ? 'gap-1' : 'gap-1.5'));

// Responsive size classes
const sizeClass = 'kpi-stat';
const labelSizeClass = 'text-[10px] sm:text-xs';
const valueSizeClass = 'text-base sm:text-lg';
</script>

<style scoped>
/* Mobile: reduce padding */
@media (max-width: 640px) {
  .kpi-stat {
    padding: 0.625rem !important;
  }
}
.kpi-skeleton {
  position: relative;
  overflow: hidden;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 4px;
}

.kpi-skeleton::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(148, 163, 184, 0.08) 50%,
    transparent 100%
  );
  animation: kpi-shimmer 1.5s ease-in-out infinite;
}

.kpi-skeleton--value {
  height: 24px;
  width: 70%;
}

.kpi-skeleton--note {
  height: 14px;
  width: 50%;
  margin-top: 4px;
}

@keyframes kpi-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
