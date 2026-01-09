<template>
  <Teleport to="body">
    <Transition name="drawer-overlay">
      <div
        v-if="modelValue"
        class="drawer-overlay fixed inset-0 bg-black/60 backdrop-blur-sm z-40"
        @click="close"
      />
    </Transition>
    <Transition name="drawer-slide">
      <aside
        v-if="modelValue"
        ref="drawerRef"
        class="drawer fixed top-0 left-0 bottom-0 w-[280px] bg-[var(--panel-strong)] border-r border-[var(--line)] z-50 flex flex-col shadow-2xl"
        :style="drawerStyle"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
      >
        <div class="drawer-header flex items-center justify-between p-5 border-b border-[var(--line)]">
          <div class="brand flex items-center gap-3">
            <div class="logo">CS</div>
            <div>
              <h1 class="text-lg font-bold">ChanScope</h1>
              <span class="text-xs text-[var(--muted)] uppercase tracking-wide">缠论实验室</span>
            </div>
          </div>
          <button
            @click="close"
            class="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-[rgba(148,163,184,0.1)] transition-colors"
            aria-label="关闭菜单"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <nav class="drawer-nav flex-1 overflow-y-auto p-4">
          <slot />
        </nav>
        <div class="drawer-footer p-4 border-t border-[var(--line)] text-xs text-[var(--muted)] space-y-1">
          <slot name="footer" />
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue';

defineProps({
  modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue']);

const drawerRef = ref(null);
const touchStartX = ref(0);
const touchCurrentX = ref(0);
const isDragging = ref(false);

const drawerStyle = computed(() => {
  if (!isDragging.value) return {};

  const deltaX = touchCurrentX.value - touchStartX.value;
  // 只允许向左滑动（关闭方向）
  if (deltaX < 0) {
    return {
      transform: `translateX(${deltaX}px)`,
      transition: 'none',
    };
  }
  return {};
});

const handleTouchStart = (e) => {
  touchStartX.value = e.touches[0].clientX;
  touchCurrentX.value = e.touches[0].clientX;
  isDragging.value = true;
};

const handleTouchMove = (e) => {
  if (!isDragging.value) return;

  touchCurrentX.value = e.touches[0].clientX;
  const deltaX = touchCurrentX.value - touchStartX.value;

  // 只在向左滑动时阻止默认行为
  if (deltaX < 0) {
    e.preventDefault();
  }
};

const handleTouchEnd = () => {
  if (!isDragging.value) return;

  const deltaX = touchCurrentX.value - touchStartX.value;
  const threshold = -80; // 滑动超过80px则关闭

  if (deltaX < threshold) {
    close();
  }

  isDragging.value = false;
  touchStartX.value = 0;
  touchCurrentX.value = 0;
};

const close = () => {
  emit('update:modelValue', false);
};
</script>

<style scoped>
/* Overlay transitions */
.drawer-overlay-enter-active,
.drawer-overlay-leave-active {
  transition: opacity 0.3s ease;
}

.drawer-overlay-enter-from,
.drawer-overlay-leave-to {
  opacity: 0;
}

/* Drawer slide transitions */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.3s ease;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

/* Prevent body scroll when drawer is open */
.drawer-overlay {
  touch-action: none;
}

/* Logo styling */
.logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: #f8fafc;
  font-weight: 700;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.3);
}
</style>
