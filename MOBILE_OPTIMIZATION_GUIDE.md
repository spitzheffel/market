# 移动端优化功能使用指南

## 1. 虚拟滚动 (Virtual Scrolling)

### 自动启用
虚拟滚动会在列表项超过 20 条时自动启用，无需手动配置。

### 在新组件中使用

```vue
<template>
  <VirtualList
    :items="yourDataArray"
    :item-height="120"
    :buffer="2"
    height="calc(100vh - 300px)"
    :item-key="(item) => item.id"
  >
    <template #default="{ item, index }">
      <!-- 你的卡片内容 -->
      <div class="card">
        {{ item.name }}
      </div>
    </template>
  </VirtualList>
</template>

<script setup>
import VirtualList from '@/components/common/VirtualList.vue';
import { ref } from 'vue';

const yourDataArray = ref([/* 你的数据 */]);
</script>
```

### 参数说明
- `items`: 数据数组（必需）
- `item-height`: 每个项目的高度（px），需要预估准确以获得最佳效果
- `buffer`: 上下缓冲区项目数，默认 3
- `height`: 容器高度，支持 CSS 单位
- `item-key`: 获取项目唯一键的函数或字段名

### 性能调优
- **item-height**: 设置为实际卡片高度可获得最佳滚动体验
- **buffer**: 增加缓冲区可减少白屏，但会增加 DOM 节点
- **height**: 使用固定高度或 calc() 计算高度

---

## 2. 抽屉侧滑关闭

### 使用方法
在移动设备上，用户可以：
1. 从抽屉内容区域向左滑动
2. 滑动超过 80px 后松手即可关闭抽屉
3. 滑动过程中会有实时的视觉反馈

### 技术细节
- 仅在触摸设备上生效
- 只允许向左滑动（关闭方向）
- 滑动距离不足时会自动回弹
- 不影响抽屉内的垂直滚动

### 自定义阈值
如需修改关闭阈值，编辑 `Drawer.vue`:

```javascript
const handleTouchEnd = () => {
  const deltaX = touchCurrentX.value - touchStartX.value;
  const threshold = -80; // 修改这个值（负数，单位 px）

  if (deltaX < threshold) {
    close();
  }
};
```

---

## 3. 国际化 (i18n)

### 在组件中使用

```vue
<template>
  <div>
    <h1>{{ t('markets.title') }}</h1>
    <p>{{ t('common.loading') }}</p>
    <button>{{ t('common.refresh') }}</button>
  </div>
</template>

<script setup>
import { useI18n } from '@/composables/useI18n';

const { t } = useI18n();
</script>
```

### 添加新的翻译词条

1. 编辑 `frontend/src/i18n/locales/zh-CN.json`:
```json
{
  "yourModule": {
    "title": "标题",
    "description": "描述"
  }
}
```

2. 编辑 `frontend/src/i18n/locales/en-US.json`:
```json
{
  "yourModule": {
    "title": "Title",
    "description": "Description"
  }
}
```

3. 在组件中使用:
```vue
{{ t('yourModule.title') }}
```

### 动态内容翻译

```javascript
// 方法 1: 使用计算属性
const items = ref([
  { id: 1, type: 'spot' },
  { id: 2, type: 'futures' }
]);

const itemsWithLabels = computed(() => {
  return items.value.map(item => ({
    ...item,
    typeLabel: t(`markets.${item.type}`)
  }));
});

// 方法 2: 使用函数
const getTypeLabel = (type) => {
  return t(`markets.${type}`);
};
```

### 语言切换

```vue
<template>
  <LanguageSwitcher />
</template>

<script setup>
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue';
</script>
```

或手动切换:

```javascript
import { useI18n } from '@/composables/useI18n';

const { locale, setLocale } = useI18n();

// 切换到英文
setLocale('en-US');

// 切换到中文
setLocale('zh-CN');

// 获取当前语言
console.log(locale.value); // 'zh-CN' 或 'en-US'
```

### 可用语言列表

```javascript
const { availableLocales } = useI18n();

// [
//   { value: 'zh-CN', label: '简体中文' },
//   { value: 'en-US', label: 'English' }
// ]
```

---

## 4. 组合使用示例

### 带虚拟滚动和 i18n 的列表

```vue
<template>
  <section class="card">
    <h2>{{ t('markets.title') }}</h2>

    <VirtualList
      v-if="items.length > 20"
      :items="itemsWithLabels"
      :item-height="100"
      height="500px"
      :item-key="(item) => item.id"
    >
      <template #default="{ item }">
        <div class="item-card">
          <h3>{{ item.name }}</h3>
          <p>{{ item.typeLabel }}</p>
        </div>
      </template>
    </VirtualList>

    <div v-else>
      <div v-for="item in itemsWithLabels" :key="item.id" class="item-card">
        <h3>{{ item.name }}</h3>
        <p>{{ item.typeLabel }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useI18n } from '@/composables/useI18n';
import VirtualList from '@/components/common/VirtualList.vue';

const { t } = useI18n();

const items = ref([/* 你的数据 */]);

const itemsWithLabels = computed(() => {
  return items.value.map(item => ({
    ...item,
    typeLabel: t(`markets.${item.type}`)
  }));
});
</script>
```

---

## 5. 最佳实践

### 虚拟滚动
- ✅ 用于大列表（20+ 项）
- ✅ 确保 item-height 准确
- ✅ 使用固定高度的项目
- ❌ 避免在项目内使用复杂的动态高度

### 手势支持
- ✅ 在移动设备上测试
- ✅ 确保不与页面滚动冲突
- ❌ 避免在抽屉内添加横向滚动

### 国际化
- ✅ 所有用户可见文本都应使用 t()
- ✅ 按功能模块组织词条
- ✅ 使用描述性的键名
- ❌ 避免在词条中硬编码数字或格式

---

## 6. 故障排查

### 虚拟滚动不工作
- 检查 items 数组是否正确传递
- 检查 item-height 是否设置
- 检查容器是否有固定高度

### 手势冲突
- 检查是否有其他 touch 事件监听器
- 检查 CSS touch-action 属性
- 尝试调整滑动阈值

### i18n 显示键名而非翻译
- 检查词条是否存在于 locale 文件中
- 检查键名拼写是否正确
- 检查 i18n 是否正确初始化

### 语言切换不生效
- 检查 localStorage 是否可用
- 检查浏览器控制台是否有错误
- 尝试清除浏览器缓存

---

## 7. 性能监控

### 虚拟滚动性能

```javascript
// 监控渲染的项目数
watch(visibleItems, (items) => {
  console.log('Visible items:', items.length);
});

// 监控滚动性能
let lastScrollTime = 0;
const handleScroll = () => {
  const now = Date.now();
  const fps = 1000 / (now - lastScrollTime);
  console.log('Scroll FPS:', fps);
  lastScrollTime = now;
};
```

### i18n 性能

```javascript
// 监控翻译调用
const originalT = t;
const t = (...args) => {
  console.time('i18n');
  const result = originalT(...args);
  console.timeEnd('i18n');
  return result;
};
```

---

## 8. 扩展和自定义

### 添加新语言

1. 创建新的 locale 文件: `frontend/src/i18n/locales/ja-JP.json`
2. 在 `frontend/src/i18n/index.js` 中导入:
```javascript
import jaJP from './locales/ja-JP.json';

const i18n = createI18n({
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'ja-JP': jaJP, // 新增
  },
});
```

3. 在 `useI18n.js` 中添加到可用语言列表:
```javascript
const availableLocales = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' },
  { value: 'ja-JP', label: '日本語' }, // 新增
];
```

### 自定义虚拟滚动行为

```vue
<!-- 自定义缓冲区大小 -->
<VirtualList :buffer="5" />

<!-- 自定义容器样式 -->
<VirtualList class="custom-scroll" />

<style>
.custom-scroll {
  scrollbar-width: thin;
  scrollbar-color: var(--accent) transparent;
}
</style>
```

---

## 需要帮助？

如有问题或建议，请查看:
- 项目文档: `MOBILE_OPTIMIZATION_SUMMARY.md`
- 源代码注释
- Vue 3 文档: https://vuejs.org/
- vue-i18n 文档: https://vue-i18n.intlify.dev/
