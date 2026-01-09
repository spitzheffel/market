<template>
  <div class="panel-header flex flex-wrap items-start justify-between gap-3 sm:gap-4">
    <div class="space-y-1">
      <h2 class="leading-tight">{{ title }}</h2>
      <p v-if="subtitle" class="panel-sub">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
});
</script>
