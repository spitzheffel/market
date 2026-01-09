<template>
  <div class="dropdown-menu relative" ref="dropdownRef">
    <button
      @click="isOpen = !isOpen"
      class="dropdown-trigger w-10 h-10 rounded-lg flex items-center justify-center hover:bg-[rgba(148,163,184,0.1)] active:bg-[rgba(148,163,184,0.2)] transition-colors"
      :aria-label="label"
      :aria-expanded="isOpen"
    >
      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="1" />
        <circle cx="12" cy="5" r="1" />
        <circle cx="12" cy="19" r="1" />
      </svg>
    </button>
    <Transition name="dropdown">
      <div
        v-if="isOpen"
        class="dropdown-content absolute right-0 top-full mt-2 w-48 bg-[var(--panel-strong)] border border-[var(--line)] rounded-xl shadow-2xl overflow-hidden z-50"
      >
        <slot />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

defineProps({
  label: { type: String, default: '更多选项' },
});

const isOpen = ref(false);
const dropdownRef = ref(null);

const handleClickOutside = (event) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.dropdown-content {
  backdrop-filter: blur(12px);
}
</style>
