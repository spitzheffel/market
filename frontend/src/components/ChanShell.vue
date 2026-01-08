<template>
  <div class="chan-app">
    <div :class="appClass">
      <aside class="sidebar card flex flex-col gap-4 p-5 col-span-1 row-auto lg:row-span-3">
        <div class="brand flex items-center gap-3">
          <div class="logo">CS</div>
          <div>
            <h1>ChanScope</h1>
            <span>缠论实验室</span>
          </div>
        </div>
        <nav class="nav flex flex-col gap-2">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item flex items-center gap-2.5 px-3 py-2 rounded-xl"
            :class="{ active: sectionKey === item.key }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path v-for="(p, idx) in item.icon" :key="idx" :d="p" />
            </svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
        <div class="sidebar-footer mt-auto p-3 text-xs space-y-1">
          数据源：Binance 现货
          <div class="mono">同步 1m | 98.7%</div>
        </div>
      </aside>

      <header :class="topbarClass">
        <div class="ticker flex items-center gap-3 px-4 py-2">
          <span class="live-dot"></span>
          <span class="pair">{{ ticker.pair }}</span>
          <span>{{ ticker.price }}</span>
          <span :class="['chg', ticker.changeDir]">{{ ticker.change }}</span>
          <span>|</span>
          <span>{{ ticker.source }}</span>
          <span>{{ ticker.interval }}</span>
        </div>
        <div class="top-actions flex items-center gap-2">
          <button class="select-pill">
            <span>标的</span>
            {{ ticker.pair }}
          </button>
          <button class="select-pill">
            <span>周期</span>
            {{ ticker.interval }}
          </button>
          <button class="select-pill">
            <span>模型</span>
            Param-A
          </button>
          <button class="button">新建预警</button>
        </div>
      </header>

      <main :class="mainClass">
        <slot />
      </main>

      <aside v-if="hasRight" :class="rightClass">
        <slot name="right" />
      </aside>

      <section v-if="hasBottom" :class="bottomClass">
        <slot name="bottom" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import '../assets/chan-dashboard.css';

const props = defineProps({
  section: { type: String, default: '' },
  hasRight: { type: Boolean, default: true },
  hasBottom: { type: Boolean, default: false },
  ticker: {
    type: Object,
    default: () => ({
      pair: 'BTC/USDT',
      price: '67,420.5',
      change: '+2.41%',
      changeDir: 'up',
      source: 'Binance 现货',
      interval: '1m',
    }),
  },
});

const route = useRoute();

const navItems = [
  { key: 'home', label: '总览', path: '/', icon: ['M4 12h7l3-6 6 12', 'M4 19h16'] },
  { key: 'markets', label: '市场', path: '/markets', icon: ['M4 6h16', 'M4 12h10', 'M4 18h12'] },
  { key: 'watchlist', label: '自选池', path: '/watchlist', icon: ['M4 6h16', 'M4 12h10', 'M4 18h12'] },
  { key: 'engine', label: '缠论引擎', path: '/engine', icon: ['M5 4v16', 'M5 16l4-4 4 3 6-8'] },
  { key: 'signals', label: '信号', path: '/signals', icon: ['M12 3l7 4v10l-7 4-7-4V7z', 'M12 12l7-4'] },
  { key: 'strategy', label: '策略实验室', path: '/strategy', icon: ['M4 7h16', 'M4 12h16', 'M4 17h16'] },
  { key: 'backtest', label: '回测', path: '/backtest', icon: ['M4 4h16v16H4z', 'M8 10h8', 'M8 14h5'] },
  { key: 'autotrade', label: '自动交易', path: '/autotrade', icon: ['M12 4v16', 'M7 9l5-5 5 5', 'M7 15l5 5 5-5'] },
  { key: 'settings', label: '设置', path: '/settings', icon: ['M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0', 'M19 12a7 7 0 0 0-.1-1.1l2-1.5-2-3.4-2.3.6a7 7 0 0 0-1.9-1.1L14 2h-4l-.7 2.5a7 7 0 0 0-1.9 1.1l-2.3-.6-2 3.4 2 1.5A7 7 0 0 0 5 12c0 .4 0 .8.1 1.1l-2 1.5 2 3.4 2.3-.6a7 7 0 0 0 1.9 1.1L10 22h4l.7-2.5a7 7 0 0 0 1.9-1.1l2.3.6 2-3.4-2-1.5c.1-.3.1-.7.1-1.1z'] },
];

const sectionKey = computed(() => props.section || route.meta.section || 'home');

const gridCols = computed(() =>
  props.hasRight || props.hasBottom
    ? 'lg:grid-cols-[260px_minmax(0,_1fr)_320px]'
    : 'lg:grid-cols-[260px_minmax(0,_1fr)]'
);
const gridRows = computed(() =>
  props.hasBottom ? 'lg:grid-rows-[76px_minmax(0,_1fr)_auto]' : 'lg:grid-rows-[76px_minmax(0,_1fr)]'
);
const appClass = computed(
  () => `app grid gap-4 p-4 sm:p-5 min-h-screen w-full grid-cols-1 ${gridCols.value} ${gridRows.value}`
);

const topbarClass = computed(() =>
  [
    'topbar card flex items-center justify-between px-4 py-3 gap-4 col-span-1 row-auto',
    props.hasRight || props.hasBottom ? 'lg:col-start-2 lg:col-end-4 lg:row-start-1' : 'lg:col-start-2 lg:col-end-3 lg:row-start-1',
  ].join(' ')
);
const mainClass = computed(
  () => 'main grid gap-4 col-span-1 row-auto lg:col-start-2 lg:col-end-3 lg:row-start-2 grid-rows-[minmax(0,_1fr)_auto]'
);
const rightClass = computed(
  () =>
    `right flex flex-col gap-4 col-span-1 row-auto ${
      props.hasBottom ? 'lg:col-start-3 lg:col-end-4 lg:row-start-2 lg:row-end-4' : 'lg:col-start-3 lg:col-end-4 lg:row-start-2 lg:row-end-3'
    }`
);
const bottomClass = computed(
  () => 'bottom card flex flex-col gap-4 p-5 col-span-1 row-auto lg:col-start-2 lg:col-end-3 lg:row-start-3'
);
</script>
