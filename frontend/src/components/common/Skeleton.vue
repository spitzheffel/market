<template>
  <div class="skeleton" :class="[`skeleton--${variant}`]" :style="customStyle">
    <div class="skeleton__shimmer" />
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  variant: {
    type: String,
    default: 'text',
    validator: (v) => ['text', 'title', 'avatar', 'button', 'card', 'table-row', 'kpi'].includes(v),
  },
  width: { type: String, default: '' },
  height: { type: String, default: '' },
  rounded: { type: String, default: '' },
});

const customStyle = computed(() => ({
  ...(props.width && { width: props.width }),
  ...(props.height && { height: props.height }),
  ...(props.rounded && { borderRadius: props.rounded }),
}));
</script>

<style scoped>
.skeleton {
  position: relative;
  overflow: hidden;
  background: rgba(148, 163, 184, 0.1);
  border-radius: var(--radius-sm);
}

.skeleton__shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(148, 163, 184, 0.08) 50%,
    transparent 100%
  );
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
}

/* Variants */
.skeleton--text {
  height: 14px;
  width: 100%;
  border-radius: 4px;
}

.skeleton--title {
  height: 24px;
  width: 60%;
  border-radius: 6px;
}

.skeleton--avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.skeleton--button {
  height: 36px;
  width: 100px;
  border-radius: 999px;
}

.skeleton--card {
  height: 120px;
  width: 100%;
  border-radius: var(--radius-md);
}

.skeleton--table-row {
  height: 44px;
  width: 100%;
  border-radius: 4px;
}

.skeleton--kpi {
  height: 80px;
  width: 100%;
  border-radius: var(--radius-sm);
}

@keyframes skeleton-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
