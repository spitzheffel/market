<template>
  <div class="chan-app">
    <div :class="appClass">
      <!-- Sidebar - hidden on mobile -->
      <aside :class="sidebarClass">
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
          {{ t('shell.dataSource') }}：{{ dataSource }}
          <div class="mono">{{ t('shell.sync') }} {{ syncInterval }} | {{ syncRate }}</div>
        </div>
      </aside>

      <!-- Mobile Drawer -->
      <Drawer v-model="drawerOpen">
        <nav class="nav flex flex-col gap-2">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item flex items-center gap-2.5 px-3 py-2 rounded-xl"
            :class="{ active: sectionKey === item.key }"
            @click="drawerOpen = false"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path v-for="(p, idx) in item.icon" :key="idx" :d="p" />
            </svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
        <div class="px-3 py-4 border-t border-[var(--line)] mt-4">
          <LanguageSwitcher />
        </div>
        <template #footer>
          {{ t('shell.dataSource') }}：{{ dataSource }}
          <div class="mono">{{ t('shell.sync') }} {{ syncInterval }} | {{ syncRate }}</div>
        </template>
      </Drawer>

      <header :class="topbarClass">
        <!-- Mobile menu button -->
        <button
          @click="drawerOpen = true"
          class="menu-button md:hidden w-10 h-10 rounded-lg flex items-center justify-center hover:bg-[rgba(148,163,184,0.1)] active:bg-[rgba(148,163,184,0.2)] transition-colors flex-shrink-0"
          :aria-label="t('shell.openMenu')"
        >
          <svg class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12h18M3 6h18M3 18h18" />
          </svg>
        </button>

        <!-- Ticker - responsive layout -->
        <div class="ticker flex flex-wrap md:flex-nowrap flex-1 min-w-0 items-center gap-2 sm:gap-3 px-3 sm:px-4 py-2 text-xs sm:text-sm overflow-hidden">
          <span class="live-dot"></span>
          <span class="pair">{{ ticker.pair }}</span>
          <span class="font-mono">{{ ticker.price }}</span>
          <span :class="['chg', ticker.changeDir]">{{ ticker.change }}</span>
          <span class="hidden sm:inline">|</span>
          <span class="text-[var(--muted)] text-[11px] sm:text-xs">{{ ticker.source }}</span>
          <span class="text-[var(--muted)] text-[11px] sm:text-xs">{{ ticker.interval }}</span>
        </div>

        <!-- Tablet actions -->
        <div class="top-actions hidden sm:flex lg:hidden items-center gap-2">
          <button class="select-pill">
            <span>{{ t('shell.symbol') }}</span>
            {{ ticker.pair }}
          </button>
        </div>

        <!-- Desktop actions -->
        <div class="top-actions hidden lg:flex items-center gap-2">
          <button class="select-pill">
            <span>{{ t('shell.symbol') }}</span>
            {{ ticker.pair }}
          </button>
          <button class="select-pill">
            <span>{{ t('shell.interval') }}</span>
            {{ ticker.interval }}
          </button>
          <button class="select-pill">
            <span>{{ t('shell.model') }}</span>
            {{ modelName }}
          </button>
          <button class="button">{{ t('shell.newAlert') }}</button>
          <LanguageSwitcher />
        </div>

        <!-- Mobile/Tablet dropdown menu -->
        <DropdownMenu :label="t('shell.moreOptions')" class="lg:hidden">
          <button class="dropdown-item w-full px-4 py-3 text-left text-sm hover:bg-[rgba(148,163,184,0.1)] active:bg-[rgba(148,163,184,0.15)] transition-colors flex items-center gap-3">
            <span class="text-[var(--muted)] text-xs">{{ t('shell.interval') }}</span>
            <span class="font-mono">{{ ticker.interval }}</span>
          </button>
          <button class="dropdown-item w-full px-4 py-3 text-left text-sm hover:bg-[rgba(148,163,184,0.1)] active:bg-[rgba(148,163,184,0.15)] transition-colors flex items-center gap-3">
            <span class="text-[var(--muted)] text-xs">{{ t('shell.model') }}</span>
            <span class="font-mono">{{ modelName }}</span>
          </button>
          <button class="dropdown-item w-full px-4 py-3 text-left text-sm hover:bg-[rgba(148,163,184,0.1)] active:bg-[rgba(148,163,184,0.15)] transition-colors border-t border-[var(--line)]">
            {{ t('shell.newAlert') }}
          </button>
          <div class="p-3 border-t border-[var(--line)]">
            <LanguageSwitcher />
          </div>
        </DropdownMenu>
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
import { computed, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useI18n } from '../composables/useI18n';
import Drawer from './common/Drawer.vue';
import DropdownMenu from './common/DropdownMenu.vue';
import LanguageSwitcher from './common/LanguageSwitcher.vue';
import '../assets/chan-dashboard.css';

const drawerOpen = ref(false);

const props = defineProps({
  section: { type: String, default: '' },
  hasRight: { type: Boolean, default: true },
  hasBottom: { type: Boolean, default: false },
  // Ticker data
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
  // Sidebar footer data
  dataSource: { type: String, default: 'Binance 现货' },
  syncInterval: { type: String, default: '1m' },
  syncRate: { type: String, default: '98.7%' },
  // Top bar model name
  modelName: { type: String, default: 'Param-A' },
});

const route = useRoute();
const { t } = useI18n();

const navItems = computed(() => ([
  // 总览 - 仪表盘/网格图标
  { key: 'home', label: t('nav.home'), path: '/', icon: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M3 14h7v7H3z', 'M14 14h7v7h-7z'] },
  // 市场 - K线柱状图
  { key: 'markets', label: t('nav.markets'), path: '/markets', icon: ['M6 20V10', 'M12 20V4', 'M18 20v-6'] },
  // 自选池 - 星标收藏
  { key: 'watchlist', label: t('nav.watchlist'), path: '/watchlist', icon: ['M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'] },
  // 图表分析 - 图表/K线图
  { key: 'chart', label: t('nav.chart'), path: '/chart', icon: ['M3 3v18h18', 'M18 17V9', 'M13 17V5', 'M8 17v-3'] },
  // 缠论引擎 - 齿轮/引擎
  { key: 'engine', label: t('nav.engine'), path: '/engine', icon: ['M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0', 'M12 2v2', 'M12 20v2', 'M4.93 4.93l1.41 1.41', 'M17.66 17.66l1.41 1.41', 'M2 12h2', 'M20 12h2', 'M4.93 19.07l1.41-1.41', 'M17.66 6.34l1.41-1.41'] },
  // 信号 - 雷达/信号波
  { key: 'signals', label: t('nav.signals'), path: '/signals', icon: ['M12 20a8 8 0 1 0 0-16', 'M12 16a4 4 0 1 0 0-8', 'M12 12h.01'] },
  // 策略实验室 - 烧杯/实验瓶
  { key: 'strategy', label: t('nav.strategy'), path: '/strategy', icon: ['M9 3h6v2H9z', 'M10 5v4l-4 8h12l-4-8V5', 'M8 14h8'] },
  // 回测 - 时钟回退/历史
  { key: 'backtest', label: t('nav.backtest'), path: '/backtest', icon: ['M3 12a9 9 0 1 0 9-9', 'M3 3v6h6', 'M12 7v5l3 3'] },
  // 自动交易 - 闪电/自动化
  { key: 'autotrade', label: t('nav.autoTrade'), path: '/autotrade', icon: ['M13 2L3 14h9l-1 8 10-12h-9l1-8z'] },
  // 设置 - 齿轮
  { key: 'settings', label: t('nav.settings'), path: '/settings', icon: ['M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0', 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1.08-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1.08 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z'] },
]));

const sectionKey = computed(() => props.section || route.meta.section || 'home');

// Grid classes based on layout mode
const appClass = computed(() => {
  const base = 'app grid gap-4 p-4 sm:p-5 min-h-screen w-full';

  // Columns: mobile 1, tablet 2 (sidebar + main), desktop optionally 3 (right panel)
  const cols = props.hasRight
    ? 'grid-cols-1 md:grid-cols-[220px_minmax(0,_1fr)] lg:grid-cols-[260px_minmax(0,_1fr)_320px]'
    : 'grid-cols-1 md:grid-cols-[220px_minmax(0,_1fr)] lg:grid-cols-[260px_minmax(0,_1fr)]';

  // Rows: keep topbar fixed at md+, stack extra panels under main on tablet
  const mdRows = props.hasRight && props.hasBottom
    ? 'md:grid-rows-[76px_minmax(0,_1fr)_auto_auto]'
    : (props.hasRight || props.hasBottom)
      ? 'md:grid-rows-[76px_minmax(0,_1fr)_auto]'
      : 'md:grid-rows-[76px_minmax(0,_1fr)]';

  // Desktop rows only depend on bottom panel (right is a column at lg+)
  const lgRows = props.hasBottom
    ? 'lg:grid-rows-[76px_minmax(0,_1fr)_auto]'
    : 'lg:grid-rows-[76px_minmax(0,_1fr)]';

  return `${base} ${cols} ${mdRows} ${lgRows}`;
});

const sidebarClass = computed(() => {
  const base = 'sidebar card flex-col gap-4 p-5 col-span-1 hidden md:flex md:col-start-1 md:row-start-1';

  const mdSpan = props.hasRight && props.hasBottom
    ? 'md:row-span-4'
    : (props.hasRight || props.hasBottom)
      ? 'md:row-span-3'
      : 'md:row-span-2';

  const lgSpan = props.hasBottom ? 'lg:row-span-3' : 'lg:row-span-2';

  return `${base} ${mdSpan} ${lgSpan}`;
});

const topbarClass = computed(() => {
  const base = 'topbar card flex items-center justify-between px-3 sm:px-4 py-3 gap-2 sm:gap-4 col-span-1 row-auto';
  const positioning = props.hasRight
    ? 'md:col-start-2 md:col-end-3 md:row-start-1 lg:col-end-4'
    : 'md:col-start-2 md:col-end-3 md:row-start-1';
  return `${base} ${positioning}`;
});

const mainClass = 'main min-w-0 grid gap-4 col-span-1 row-auto md:col-start-2 md:col-end-3 md:row-start-2 grid-rows-[minmax(0,_1fr)_auto]';

const rightClass = computed(() => {
  const base = 'right min-w-0 flex flex-col gap-4 col-span-1 row-auto';
  const mdPos = 'md:col-start-2 md:col-end-3 md:row-start-3';
  const lgPos = 'lg:col-start-3 lg:col-end-4 lg:row-start-2';
  const lgSpan = props.hasBottom ? 'lg:row-end-4' : 'lg:row-end-3';
  return `${base} ${mdPos} ${lgPos} ${lgSpan}`;
});

const bottomClass = computed(() => {
  const base = 'bottom card flex flex-col gap-4 p-4 sm:p-5 col-span-1 row-auto md:col-start-2 md:col-end-3';
  const mdRow = props.hasRight ? 'md:row-start-4' : 'md:row-start-3';
  const lgRow = 'lg:row-start-3';
  return `${base} ${mdRow} ${lgRow}`;
});
</script>
