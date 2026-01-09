<template>
  <div class="empty-state" :class="[`empty-state--${size}`]">
    <div v-if="$slots.icon || icon" class="empty-state__icon">
      <slot name="icon">
        <!-- Default empty icon -->
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        </svg>
      </slot>
    </div>
    <div class="empty-state__content">
      <h4 v-if="title" class="empty-state__title">{{ title }}</h4>
      <p v-if="description" class="empty-state__description">{{ description }}</p>
    </div>
    <div v-if="$slots.action" class="empty-state__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  icon: { type: Boolean, default: true },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v),
  },
});
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 16px;
}

.empty-state--sm {
  padding: 20px 12px;
}

.empty-state--lg {
  padding: 48px 24px;
}

.empty-state__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  color: var(--muted);
  opacity: 0.5;
}

.empty-state--sm .empty-state__icon {
  width: 40px;
  height: 40px;
  margin-bottom: 12px;
}

.empty-state--lg .empty-state__icon {
  width: 72px;
  height: 72px;
  margin-bottom: 20px;
}

.empty-state__icon svg {
  width: 100%;
  height: 100%;
}

.empty-state__content {
  max-width: 280px;
}

.empty-state__title {
  margin: 0 0 6px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.empty-state--sm .empty-state__title {
  font-size: 12px;
}

.empty-state--lg .empty-state__title {
  font-size: 16px;
}

.empty-state__description {
  margin: 0;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}

.empty-state--sm .empty-state__description {
  font-size: 11px;
}

.empty-state--lg .empty-state__description {
  font-size: 13px;
}

.empty-state__action {
  margin-top: 16px;
}

.empty-state--sm .empty-state__action {
  margin-top: 12px;
}

.empty-state--lg .empty-state__action {
  margin-top: 20px;
}
</style>
