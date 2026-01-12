<template>
  <div class="language-switcher">
    <button
      v-for="lang in availableLocales"
      :key="lang.value"
      :class="['lang-button', { active: locale === lang.value }]"
      @click="setLocale(lang.value)"
    >
      {{ lang.label }}
    </button>
  </div>
</template>

<script setup>
import { useI18n } from '../../composables/useI18n';

const { locale, setLocale, availableLocales } = useI18n();
</script>

<style scoped>
.language-switcher {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
}

.lang-button {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--muted);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.lang-button:hover {
  background: rgba(148, 163, 184, 0.1);
  color: var(--ink);
}

.lang-button.active {
  background: var(--accent);
  color: white;
  font-weight: 600;
}
</style>
