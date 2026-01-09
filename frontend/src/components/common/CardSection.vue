<template>
  <div class="card-section" :class="{ 'card-section--bordered': bordered }">
    <div v-if="title || $slots.header" class="card-section__header">
      <slot name="header">
        <h4 class="card-section__title">{{ title }}</h4>
        <p v-if="subtitle" class="card-section__subtitle">{{ subtitle }}</p>
      </slot>
    </div>
    <div class="card-section__content" :class="[layoutClass]">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  bordered: { type: Boolean, default: false },
  /**
   * Layout options:
   * - stack: vertical stack (default)
   * - grid-2: 2 column grid
   * - grid-3: 3 column grid
   * - row: horizontal flex row
   */
  layout: {
    type: String,
    default: 'stack',
    validator: (v) => ['stack', 'grid-2', 'grid-3', 'row'].includes(v),
  },
  gap: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v),
  },
});

const layoutClass = computed(() => {
  const classes = [`card-section__content--${props.layout}`, `card-section__content--gap-${props.gap}`];
  return classes;
});
</script>

<style scoped>
.card-section {
  display: flex;
  flex-direction: column;
}

.card-section--bordered {
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.card-section__header {
  margin-bottom: 12px;
}

.card-section__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.3px;
}

.card-section__subtitle {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--muted);
}

/* Content layouts */
.card-section__content--stack {
  display: flex;
  flex-direction: column;
}

.card-section__content--row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.card-section__content--grid-2 {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
}

.card-section__content--grid-3 {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
}

@media (min-width: 640px) {
  .card-section__content--grid-2 {
    grid-template-columns: repeat(2, 1fr);
  }

  .card-section__content--grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 768px) {
  .card-section__content--grid-3 {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Gap sizes */
.card-section__content--gap-sm {
  gap: 8px;
}

.card-section__content--gap-md {
  gap: 12px;
}

.card-section__content--gap-lg {
  gap: 16px;
}
</style>
