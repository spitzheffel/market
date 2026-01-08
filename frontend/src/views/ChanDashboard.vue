<template>
  <div class="chan-app">
    <div
      class="app grid gap-4 p-4 sm:p-5 min-h-screen grid-cols-1 lg:grid-cols-[240px_minmax(0,_1fr)_330px] lg:grid-rows-[76px_minmax(0,_1fr)_230px]"
    >
      <aside
        class="sidebar card flex flex-col gap-4 p-5 col-span-1 row-auto lg:col-[1/2] lg:row-[1/4]"
      >
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
            :class="{ active: route.meta.section === item.key }"
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

      <header
        class="topbar card flex items-center justify-between px-4 py-3 gap-4 col-span-1 row-auto lg:col-[2/4] lg:row-[1/2]"
      >
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

      <main
        class="main grid gap-4 col-span-1 row-auto lg:col-[2/3] lg:row-[2/3] grid-rows-[minmax(0,_1fr)_210px]"
      >
        <template v-if="isHome">
          <section class="chart-card card flex flex-col gap-4 p-5">
            <div class="panel-header flex items-center justify-between gap-4">
              <div>
                <h2>结构叠加</h2>
              <div class="panel-sub">笔、线段、中枢、背驰映射。</div>
            </div>
            <div class="panel-actions">
              <button class="button-ghost">叠加：开启</button>
              <button class="button-ghost">自动缩放</button>
            </div>
          </div>
          <div class="chart-shell grid grid-rows-[1fr_auto] gap-3 p-4">
            <div class="chart-grid grid grid-cols-1 xl:grid-cols-[1fr_200px] gap-4">
              <div class="chart-visual rounded-xl border border-[rgba(148,163,184,0.25)] relative overflow-hidden p-3.5">
                <svg viewBox="0 0 600 280" preserveAspectRatio="none" aria-hidden="true">
                  <rect x="0" y="0" width="600" height="280" fill="none" />
                  <path
                    d="M20 210 L70 170 L120 200 L170 120 L220 150 L270 90 L320 130 L370 80 L420 120 L470 70 L520 110 L580 50"
                    stroke="#60a5fa"
                    stroke-width="2.5"
                    fill="none"
                  />
                  <path
                    d="M20 220 L70 190 L120 220 L170 160 L220 180 L270 120 L320 150 L370 110 L420 150 L470 100 L520 140 L580 90"
                    stroke="#22d3ee"
                    stroke-width="2"
                    stroke-dasharray="6 6"
                    fill="none"
                  />
                  <rect
                    x="120"
                    y="140"
                    width="120"
                    height="70"
                    fill="rgba(245, 158, 11, 0.18)"
                    stroke="#f59e0b"
                  />
                  <rect
                    x="310"
                    y="95"
                    width="120"
                    height="70"
                    fill="rgba(245, 158, 11, 0.18)"
                    stroke="#f59e0b"
                  />
                  <g fill="#f43f5e">
                    <rect x="70" y="150" width="8" height="40" />
                    <rect x="140" y="170" width="8" height="30" />
                    <rect x="210" y="110" width="8" height="55" />
                  </g>
                  <g fill="#22c55e">
                    <rect x="95" y="140" width="8" height="55" />
                    <rect x="180" y="120" width="8" height="45" />
                    <rect x="260" y="95" width="8" height="60" />
                  </g>
                </svg>
              </div>
              <div class="chart-side flex flex-col gap-3">
                <div v-for="stat in miniStats" :key="stat.label" class="mini-card p-3">
                  {{ stat.label }}
                  <strong class="mono">{{ stat.value }}</strong>
                  <div class="panel-sub">{{ stat.note }}</div>
                </div>
              </div>
            </div>
            <div class="chart-legend flex flex-wrap gap-3 text-sm text-muted">
              <span><span class="legend-dot" style="background: #60a5fa"></span>价格</span>
              <span><span class="legend-dot" style="background: #22d3ee"></span>线段</span>
              <span><span class="legend-dot" style="background: #f59e0b"></span>中枢</span>
              <span><span class="legend-dot" style="background: #f43f5e"></span>卖点</span>
            </div>
          </div>
          <div class="chart-footer grid grid-cols-2 md:grid-cols-4 gap-3 text-xs text-muted">
            <div v-for="item in chartFooter" :key="item.label">
              {{ item.label }}: <span class="mono">{{ item.value }}</span>
            </div>
          </div>
          </section>

          <section class="levels-row grid gap-4 xl:grid-cols-[1.2fr_1fr]">
            <div class="levels-card card flex flex-col gap-3 p-4">
            <div class="panel-header flex items-center justify-between gap-4">
              <div>
                <h2>多级别快照</h2>
                <div class="panel-sub">1m / 5m / 15m 多周期联动</div>
              </div>
              <button class="button-ghost">锁定联动</button>
            </div>
            <div class="level-grid grid grid-cols-1 md:grid-cols-3 gap-3">
              <div v-for="level in levels" :key="level.level" class="level-item">
                <h4>{{ level.level }}</h4>
                <strong>{{ level.title }}</strong>
                <div class="panel-sub">{{ level.note }}</div>
              </div>
            </div>
            <div class="panel-sub">共识：买方区间形成（弱）。</div>
          </div>
          <div class="watch-card card flex flex-col gap-3 p-4">
            <div class="panel-header flex items-center justify-between gap-4">
              <div>
                <h2>自选池</h2>
                <div class="panel-sub">自动扫描缠论信号</div>
              </div>
              <button class="button-ghost">编辑</button>
            </div>
            <table>
              <thead>
                <tr>
                  <th>交易对</th>
                  <th>最新价</th>
                  <th>信号</th>
                  <th>偏向</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in watchlist" :key="row.pair">
                  <td>{{ row.pair }}</td>
                  <td class="mono">{{ row.last }}</td>
                  <td><span :class="['tag', row.signal]">{{ row.signalLabel }}</span></td>
                  <td class="mono">{{ row.bias }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        </template>

        <template v-else>
          <section class="card p-6 flex flex-col gap-3">
            <div class="text-muted text-sm uppercase tracking-wide">占位页面</div>
            <h2 class="text-2xl font-semibold text-white">{{ currentTitle }}</h2>
            <p class="text-muted text-sm">{{ currentDesc }}</p>
            <div class="flex gap-3 mt-2">
              <button class="button" @click="goHome">返回总览</button>
            </div>
          </section>
        </template>
      </main>

      <aside
        v-if="isHome"
        class="right flex flex-col gap-4 col-span-1 row-auto lg:col-[3/4] lg:row-[2/4]"
      >
        <section class="signal-card card flex flex-col gap-3 p-4">
          <div class="panel-header flex items-center justify-between gap-4">
            <div>
              <h2>信号监控</h2>
              <div class="panel-sub">实时队列与置信度</div>
            </div>
            <button class="button-ghost">筛选</button>
          </div>
          <div
            v-for="signal in signalFeed"
            :key="signal.code"
            class="signal-item grid grid-cols-[auto,1fr,auto] items-center gap-3 p-3"
          >
            <div :class="['tag', signal.tag]">{{ signal.code }}</div>
            <div>
              <div class="signal-title">{{ signal.title }}</div>
              <div class="signal-meta">{{ signal.meta }}</div>
            </div>
            <div class="mono">{{ signal.time }}</div>
          </div>
        </section>

        <section class="risk-card card flex flex-col gap-3 p-4">
          <div class="panel-header flex items-center justify-between gap-4">
            <div>
              <h2>风险与持仓</h2>
              <div class="panel-sub">自动交易风控</div>
            </div>
          </div>
          <div class="risk-meter"><span></span></div>
          <div class="panel-sub">仓位：62% | 最大回撤：3.4%</div>
          <div class="mini-card p-3">
            持仓数量
            <strong class="mono">3</strong>
            <div class="panel-sub">BTC, ETH, SOL</div>
          </div>
        </section>

        <section class="session-card card flex flex-col gap-3 p-4">
          <div class="panel-header flex items-center justify-between gap-4">
            <div>
              <h2>交易日历</h2>
              <div class="panel-sub">Binance 24/7 | 维护提示</div>
            </div>
          </div>
          <div class="session-list flex flex-col gap-2">
            <div
              v-for="session in sessions"
              :key="session.label"
              class="session-item flex justify-between text-sm text-muted"
            >
              <span>{{ session.label }}</span>
              <span class="mono">{{ session.value }}</span>
            </div>
          </div>
        </section>
      </aside>

      <section
        v-if="isHome"
        class="bottom card flex flex-col gap-4 p-5 col-span-1 row-auto lg:col-[2/3] lg:row-[3/4]"
      >
        <div class="panel-header flex items-center justify-between gap-4">
          <div>
            <h2>策略实验室</h2>
            <div class="panel-sub">分级参数化缠论规则</div>
          </div>
          <div class="strategy-actions flex items-center gap-3">
            <button class="button-ghost">保存模板</button>
            <button class="button">运行回测</button>
          </div>
        </div>
        <div class="strategy-grid grid grid-cols-1 md:grid-cols-3 gap-3">
          <div v-for="param in strategyParams" :key="param.label" class="param p-3">
            <label>{{ param.label }}</label>
            <div class="value">{{ param.value }}</div>
            <input type="range" :min="param.min" :max="param.max" :value="param.value" />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import '../assets/chan-dashboard.css';

const route = useRoute();
const router = useRouter();

const ticker = {
  pair: 'BTC/USDT',
  price: '67,420.5',
  change: '+2.41%',
  changeDir: 'up',
  source: 'Binance 现货',
  interval: '1m',
};

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

const placeholderMap = {
  markets: { title: '市场', desc: '市场看板正在建设中，后续提供多市场联动与过滤。' },
  watchlist: { title: '自选池', desc: '自选池管理与批量订阅配置即将上线。' },
  engine: { title: '缠论引擎', desc: '缠论规则配置与回放面板筹备中。' },
  signals: { title: '信号', desc: '信号列表与过滤器即将接入。' },
  strategy: { title: '策略实验室', desc: '策略参数模板与回测入口将在此呈现。' },
  backtest: { title: '回测', desc: '回测任务与报告面板开发中。' },
  autotrade: { title: '自动交易', desc: '交易通道与风控策略对接中。' },
  settings: { title: '设置', desc: '偏好、风控、账户绑定配置即将上线。' },
};

const isHome = computed(() => route.meta.section === 'home');
const currentTitle = computed(() => placeholderMap[route.meta.section]?.title || '即将上线');
const currentDesc = computed(() => placeholderMap[route.meta.section]?.desc || '该页面正在建设中。');

const goHome = () => router.push('/');

const miniStats = [
  { label: '突破力度', value: '0.78', note: '线段 3 对比 1' },
  { label: '背驰指数', value: '+12.4', note: '动能减弱' },
  { label: '中枢同步', value: '3 / 4', note: '多级别对齐' },
];

const chartFooter = [
  { label: '结构更新时间', value: '00:00:47' },
  { label: '最新确认笔', value: 'S-28' },
  { label: '线段斜率', value: '+1.8%' },
  { label: '中枢重叠', value: '0.62' },
];

const levels = [
  { level: '1m', title: '笔 42', note: '中枢 6 | 趋势上' },
  { level: '5m', title: '线段 12', note: '中枢 3 | 趋势盘整' },
  { level: '15m', title: '线段 6', note: '中枢 2 | 趋势上' },
];

const watchlist = [
  { pair: 'ETH/USDT', last: '3,492.1', signal: 'buy', signalLabel: '买', bias: '+1.2%' },
  { pair: 'SOL/USDT', last: '142.8', signal: 'wait', signalLabel: '观望', bias: '+0.3%' },
  { pair: 'BNB/USDT', last: '612.4', signal: 'sell', signalLabel: '卖', bias: '-0.9%' },
  { pair: 'ARB/USDT', last: '1.16', signal: 'wait', signalLabel: '观望', bias: '+0.1%' },
];

const signalFeed = [
  { tag: 'buy', code: 'B1', title: 'BTC/USDT | 1m', meta: '中枢突破 | 0.71', time: '00:48' },
  { tag: 'wait', code: 'S2', title: 'ETH/USDT | 5m', meta: '弱背驰', time: '02:14' },
  { tag: 'sell', code: 'S1', title: 'BNB/USDT | 15m', meta: '线段衰竭', time: '04:02' },
];

const sessions = [
  { label: '维护窗口', value: '02:00-02:05' },
  { label: '合约资金费', value: '08:00' },
  { label: '下次扫描', value: '00:00:31' },
];

const strategyParams = [
  { label: '最小成笔K数', value: 5, min: 3, max: 9 },
  { label: '中枢重叠度', value: 62, min: 40, max: 80 },
  { label: '背驰阈值', value: 12, min: 5, max: 25 },
];
</script>
