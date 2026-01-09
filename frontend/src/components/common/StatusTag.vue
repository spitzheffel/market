<template>
  <span class="status-tag" :class="[`status-tag--${variant}`, `status-tag--${size}`]">
    <span v-if="dot" class="status-tag__dot" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  /**
   * Variant determines the color scheme:
   * - buy/success: green - 买入、完成、成功
   * - sell/danger: red - 卖出、失败、错误
   * - wait/warning: yellow - 观望、等待、警告
   * - running/info: blue - 运行中、进行中
   * - queued/neutral: gray - 排队、待处理
   */
  variant: {
    type: String,
    default: 'neutral',
    validator: (v) =>
      ['buy', 'sell', 'wait', 'running', 'queued', 'success', 'danger', 'warning', 'info', 'neutral'].includes(v),
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v),
  },
  dot: { type: Boolean, default: false },
});
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  border-radius: 999px;
  border: 1px solid transparent;
  white-space: nowrap;
}

/* Sizes */
.status-tag--sm {
  padding: 2px 8px;
  font-size: 10px;
  gap: 4px;
}

.status-tag--md {
  padding: 4px 10px;
  font-size: 11px;
}

.status-tag--lg {
  padding: 6px 14px;
  font-size: 12px;
}

/* Dot indicator */
.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-tag--sm .status-tag__dot {
  width: 5px;
  height: 5px;
}

.status-tag--lg .status-tag__dot {
  width: 8px;
  height: 8px;
}

/* Variants - Buy/Success (green) */
.status-tag--buy,
.status-tag--success {
  background: rgba(34, 197, 94, 0.15);
  color: var(--success);
  border-color: rgba(34, 197, 94, 0.4);
}

/* Variants - Sell/Danger (red) */
.status-tag--sell,
.status-tag--danger {
  background: rgba(244, 63, 94, 0.15);
  color: var(--danger);
  border-color: rgba(244, 63, 94, 0.4);
}

/* Variants - Wait/Warning (yellow) */
.status-tag--wait,
.status-tag--warning {
  background: rgba(250, 204, 21, 0.16);
  color: #facc15;
  border-color: rgba(250, 204, 21, 0.4);
}

/* Variants - Running/Info (blue) */
.status-tag--running,
.status-tag--info {
  background: rgba(59, 130, 246, 0.15);
  color: var(--accent);
  border-color: rgba(59, 130, 246, 0.4);
}

/* Variants - Queued/Neutral (gray) */
.status-tag--queued,
.status-tag--neutral {
  background: rgba(148, 163, 184, 0.15);
  color: var(--muted);
  border-color: rgba(148, 163, 184, 0.3);
}

/* Animated dot for running state */
.status-tag--running .status-tag__dot,
.status-tag--info .status-tag__dot {
  animation: status-tag-pulse 1.5s ease-in-out infinite;
}

@keyframes status-tag-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
</style>
