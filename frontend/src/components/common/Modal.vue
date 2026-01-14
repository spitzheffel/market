<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click="handleOverlay">
        <div class="modal-panel" :class="sizeClass" @click.stop>
          <header v-if="title || $slots.header" class="modal-header">
            <slot name="header">
              <h3 class="modal-title">{{ title }}</h3>
              <button class="modal-close" type="button" :aria-label="t('common.close')" @click="close">
                ✕
              </button>
            </slot>
          </header>
          <div class="modal-body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue';
import { useI18n } from '../../composables/useI18n';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v),
  },
  closeOnOverlay: { type: Boolean, default: true },
});

const emit = defineEmits(['update:modelValue']);
const { t } = useI18n();

const sizeClass = computed(() => `modal-panel--${props.size}`);

const close = () => {
  emit('update:modelValue', false);
};

const handleOverlay = () => {
  if (props.closeOnOverlay) close();
};

const handleKeydown = (event) => {
  if (event.key === 'Escape' && props.modelValue) {
    close();
  }
};

watch(
  () => props.modelValue,
  (visible) => {
    document.body.style.overflow = visible ? 'hidden' : '';
  }
);

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  document.body.style.overflow = '';
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.7);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  z-index: 60;
  padding: 24px;
}

.modal-panel {
  width: min(92vw, 720px);
  background: var(--panel-strong);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  max-height: 86vh;
}

.modal-panel--sm {
  width: min(92vw, 520px);
}

.modal-panel--lg {
  width: min(94vw, 920px);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 0;
}

.modal-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 18px;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.7);
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  color: var(--ink);
  border-color: rgba(148, 163, 184, 0.5);
}

.modal-body {
  padding: 18px 20px 20px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-footer {
  padding: 0 20px 18px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
