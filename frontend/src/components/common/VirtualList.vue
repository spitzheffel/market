<template>
  <div ref="containerRef" class="virtual-list" :style="containerStyle">
    <div class="virtual-list__spacer" :style="{ height: `${totalHeight}px` }">
      <div
        class="virtual-list__content"
        :style="{ transform: `translateY(${offsetY}px)` }"
      >
        <slot
          v-for="item in visibleItems"
          :key="getItemKey(item)"
          :item="item"
          :index="item.__index"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';

const props = defineProps({
  items: { type: Array, required: true },
  itemHeight: { type: Number, default: 100 }, // 预估每个卡片高度
  buffer: { type: Number, default: 3 }, // 上下缓冲区项目数
  height: { type: String, default: '100%' },
  minHeight: { type: String, default: '200px' }, // 最小高度，防止负值
  itemKey: { type: [String, Function], default: 'id' },
});

const containerRef = ref(null);
const scrollTop = ref(0);

// 容器样式，使用 max() 确保不会变成负值
const containerStyle = computed(() => ({
  height: `max(${props.minHeight}, ${props.height})`,
}));

// 计算总高度
const totalHeight = computed(() => props.items.length * props.itemHeight);

// 计算可见区域的起始和结束索引
const visibleRange = computed(() => {
  const container = containerRef.value;
  if (!container) return { start: 0, end: 10 };

  const viewportHeight = container.clientHeight;
  const start = Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.buffer);
  const visibleCount = Math.ceil(viewportHeight / props.itemHeight);
  const end = Math.min(props.items.length, start + visibleCount + props.buffer * 2);

  return { start, end };
});

// 计算偏移量
const offsetY = computed(() => visibleRange.value.start * props.itemHeight);

// 计算可见项目
const visibleItems = computed(() => {
  const { start, end } = visibleRange.value;
  return props.items.slice(start, end).map((item, idx) => ({
    ...item,
    __index: start + idx,
  }));
});

// 获取项目的唯一键
const getItemKey = (item) => {
  if (typeof props.itemKey === 'function') {
    return props.itemKey(item);
  }
  return item[props.itemKey] || item.__index;
};

// 滚动事件处理
const handleScroll = () => {
  if (containerRef.value) {
    scrollTop.value = containerRef.value.scrollTop;
  }
};

// 监听items变化，重置滚动位置
watch(() => props.items.length, () => {
  if (containerRef.value) {
    scrollTop.value = containerRef.value.scrollTop;
  }
});

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('scroll', handleScroll, { passive: true });
  }
});

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScroll);
  }
});
</script>

<style scoped>
.virtual-list {
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
}

.virtual-list__spacer {
  position: relative;
  width: 100%;
}

.virtual-list__content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
