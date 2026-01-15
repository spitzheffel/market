<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('chart.title')" :subtitle="t('chart.subtitle')">
      <template #actions>
        <div class="flex flex-wrap gap-2">
          <select
            v-model="selectedSymbol"
            class="select-input px-3 py-2 rounded-lg text-sm"
          >
            <option v-for="symbol in symbols" :key="symbol" :value="symbol">
              {{ symbol }}
            </option>
          </select>
        </div>
      </template>
    </CardHeader>

    <!-- 控制面板 -->
    <div class="flex flex-wrap items-center gap-3 pb-3 border-b border-[var(--line)]">
      <!-- 周期选择 -->
      <FilterGroup :label="t('chart.interval')">
        <FilterPill
          v-for="interval in intervals"
          :key="interval"
          :label="interval"
          :active="selectedInterval === interval"
          @click="selectedInterval = interval"
        />
      </FilterGroup>

      <!-- 示例提示 -->
      <div class="text-xs text-muted bg-[rgba(251,191,36,0.1)] px-3 py-1.5 rounded-full border border-[rgba(251,191,36,0.3)]">
        <span class="text-[#fbbf24]">&#9888;</span> {{ t('chart.demoNote') }}
      </div>

      <!-- 显示选项 -->
      <div class="flex flex-wrap items-center gap-3 ml-auto">
        <label class="flex items-center gap-2 text-sm cursor-pointer hover:text-accent transition-colors">
          <input
            type="checkbox"
            v-model="showOptions.fenxing"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('chart.fenxing') }}</span>
        </label>
        <label class="flex items-center gap-2 text-sm cursor-pointer hover:text-accent transition-colors">
          <input
            type="checkbox"
            v-model="showOptions.bi"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('chart.bi') }}</span>
        </label>
        <label class="flex items-center gap-2 text-sm cursor-pointer hover:text-accent transition-colors">
          <input
            type="checkbox"
            v-model="showOptions.xianduan"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('chart.xianduan') }}</span>
        </label>
        <label class="flex items-center gap-2 text-sm cursor-pointer hover:text-accent transition-colors">
          <input
            type="checkbox"
            v-model="showOptions.zhongshu"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('chart.zhongshu') }}</span>
        </label>
        <label class="flex items-center gap-2 text-sm cursor-pointer hover:text-accent transition-colors">
          <input
            type="checkbox"
            v-model="showOptions.tradingPoints"
            class="w-4 h-4 accent-[var(--accent)]"
          />
          <span>{{ t('chart.tradingPoints') }}</span>
        </label>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-wrapper">
      <div v-if="chartLoading" class="flex items-center justify-center h-full text-muted">
        {{ t('chart.loading') }}
      </div>
      <div v-else-if="chartError" class="flex items-center justify-center h-full text-muted">
        {{ chartError }}
      </div>
      <ChanChart
        v-else-if="currentChartData"
        :chart-data="currentChartData"
        :show-fenxing="showOptions.fenxing"
        :show-bi="showOptions.bi"
        :show-xianduan="showOptions.xianduan"
        :show-zhongshu="showOptions.zhongshu"
        :show-trading-points="showOptions.tradingPoints"
      />
      <div v-else class="flex items-center justify-center h-full text-muted">
        {{ t('common.empty') }}
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
      <div class="mini-card p-3">
        <div class="text-xs text-muted">{{ t('chart.fenxingCount') }}</div>
        <div class="mono text-lg font-semibold mt-1">
          {{ currentChartData?.chanTheory.fenxing.length || 0 }}
        </div>
      </div>
      <div class="mini-card p-3">
        <div class="text-xs text-muted">{{ t('chart.biCount') }}</div>
        <div class="mono text-lg font-semibold mt-1">
          {{ currentChartData?.chanTheory.bi.length || 0 }}
        </div>
      </div>
      <div class="mini-card p-3">
        <div class="text-xs text-muted">{{ t('chart.xianduanCount') }}</div>
        <div class="mono text-lg font-semibold mt-1">
          {{ currentChartData?.chanTheory.xianduan.length || 0 }}
        </div>
      </div>
      <div class="mini-card p-3">
        <div class="text-xs text-muted">{{ t('chart.zhongshuCount') }}</div>
        <div class="mono text-lg font-semibold mt-1">
          {{ currentChartData?.chanTheory.zhongshu.length || 0 }}
        </div>
      </div>
      <div class="mini-card p-3">
        <div class="text-xs text-muted">{{ t('chart.tradingPointsCount') }}</div>
        <div class="mono text-lg font-semibold mt-1">
          {{ currentChartData?.chanTheory.tradingPoints.length || 0 }}
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import FilterPill from '../components/common/FilterPill.vue';
import ChanChart from '../components/Chart/ChanChart.vue';
import { chanApi } from '../api/market';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const symbols = ['BTC/USDT', 'ETH/USDT', 'SOL/USDT', 'BNB/USDT'];
const intervals = ['1m', '5m', '15m', '1h', '4h', '1d'];

const selectedSymbol = ref('BTC/USDT');
const selectedInterval = ref('1m');

const showOptions = ref({
  fenxing: true,
  bi: true,
  xianduan: true,
  zhongshu: true,
  tradingPoints: true
});

const currentChartData = ref(null);
const chartLoading = ref(false);
const chartError = ref('');
let chartRequestId = 0;

const resolveTimestamp = (value) => {
  if (value === null || value === undefined) return null;
  if (typeof value === 'number') return value;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const formatDate = (timestamp) => {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleString(undefined, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const buildChartData = (symbol, interval, klines, result) => {
  if (!Array.isArray(klines) || klines.length === 0) return null;

  const merged = result?.mergedKlines || [];
  const mergedByIndex = new Map(
    merged.map((item) => [item.index ?? 0, item])
  );
  const rawIndexByTimestamp = new Map(
    klines.map((item, idx) => [resolveTimestamp(item.timestamp ?? item.time), idx])
  );

  const resolveMergedIndex = (mergedIndex) => {
    if (typeof mergedIndex !== 'number') return 0;
    const mergedItem = mergedByIndex.get(mergedIndex);
    if (!mergedItem) return mergedIndex;
    const ts = resolveTimestamp(mergedItem.timestamp ?? mergedItem.time);
    return rawIndexByTimestamp.get(ts) ?? mergedIndex;
  };

  // 根据时间戳查找kline索引
  const findIndexByTime = (timestamp) => {
    if (!timestamp) return 0;
    const idx = rawIndexByTimestamp.get(timestamp);
    if (idx !== undefined) return idx;
    // 查找最近的
    for (let i = 0; i < klines.length; i++) {
      const klineTime = resolveTimestamp(klines[i].timestamp ?? klines[i].time);
      if (klineTime >= timestamp) return i;
    }
    return klines.length - 1;
  };

  const klineData = klines.map((item) => {
    const timestamp = resolveTimestamp(item.timestamp ?? item.time);
    return {
      timestamp,
      date: formatDate(timestamp),
      open: String(item.open ?? ''),
      high: String(item.high ?? ''),
      low: String(item.low ?? ''),
      close: String(item.close ?? ''),
      volume: String(item.volume ?? '')
    };
  });

  const fenxing = (result?.fenxings || []).map((fx) => ({
    index: resolveMergedIndex(fx.centerIndex ?? fx.index ?? 0),
    type: fx.type === 'TOP' ? 'top' : 'bottom',
    price: String(fx.price ?? ''),
    timestamp: resolveTimestamp(fx.timestamp ?? fx.time)
  }));

  const bi = (result?.bis || []).map((stroke) => ({
    startIndex: resolveMergedIndex(stroke.startFenxing?.centerIndex ?? stroke.startIndex ?? 0),
    endIndex: resolveMergedIndex(stroke.endFenxing?.centerIndex ?? stroke.endIndex ?? 0),
    startPrice: String(stroke.startPrice ?? ''),
    endPrice: String(stroke.endPrice ?? ''),
    direction: stroke.direction === 'UP' ? 'up' : 'down'
  }));

  // 线段：后端返回startTime/endTime时间戳
  const xianduan = (result?.xianduans || []).map((xd) => ({
    startIndex: findIndexByTime(xd.startTime),
    endIndex: findIndexByTime(xd.endTime),
    startPrice: String(xd.startPrice ?? ''),
    endPrice: String(xd.endPrice ?? ''),
    direction: xd.direction === 'UP' ? 'up' : 'down'
  }));

  // 中枢：后端返回startTime/endTime时间戳
  const zhongshu = (result?.zhongshus || []).map((zs) => ({
    startIndex: findIndexByTime(zs.startTime),
    endIndex: findIndexByTime(zs.endTime),
    high: String(zs.high ?? ''),
    low: String(zs.low ?? ''),
    center: String(zs.center ?? '')
  }));

  // 买卖点：后端返回timestamp时间戳
  const tradingPoints = (result?.tradingPoints || []).map((pt) => ({
    index: findIndexByTime(pt.timestamp),
    price: String(pt.price ?? ''),
    type: pt.type === 'BUY' ? 'buy' : 'sell',
    level: pt.level ?? 1
  }));

  return {
    symbol,
    interval,
    klineData,
    chanTheory: {
      fenxing,
      bi,
      xianduan,
      zhongshu,
      tradingPoints
    }
  };
};

const syncRouteQuery = () => {
  const currentSymbol = typeof route.query.symbol === 'string' ? route.query.symbol : '';
  const currentInterval = typeof route.query.interval === 'string' ? route.query.interval : '';

  if (currentSymbol === selectedSymbol.value && currentInterval === selectedInterval.value) {
    return;
  }

  router.replace({
    query: {
      ...route.query,
      symbol: selectedSymbol.value,
      interval: selectedInterval.value
    }
  });
};

const fetchChartData = async () => {
  const requestId = ++chartRequestId;
  chartLoading.value = true;
  chartError.value = '';

  try {
    const analysis = await chanApi.getAnalysisFull({
      symbol: selectedSymbol.value,
      interval: selectedInterval.value,
      limit: 200,
      exchange: 'binance'
    });

    if (requestId !== chartRequestId) return;

    const klines = analysis?.klines || [];
    const chanData = analysis?.result || null;

    currentChartData.value = buildChartData(
      selectedSymbol.value,
      selectedInterval.value,
      klines,
      chanData
    );

    if (!currentChartData.value) {
      chartError.value = t('common.empty');
    }
  } catch (error) {
    if (requestId !== chartRequestId) return;
    console.error('Failed to load chart data', error);
    chartError.value = t('chart.loadFailed');
    currentChartData.value = null;
  } finally {
    if (requestId === chartRequestId) {
      chartLoading.value = false;
    }
  }
};

const initFromQuery = () => {
  const symbolFromQuery = typeof route.query.symbol === 'string' ? route.query.symbol : '';
  if (symbolFromQuery && symbols.includes(symbolFromQuery)) {
    selectedSymbol.value = symbolFromQuery;
  }

  const intervalFromQuery = typeof route.query.interval === 'string' ? route.query.interval : '';
  if (intervalFromQuery && intervals.includes(intervalFromQuery)) {
    selectedInterval.value = intervalFromQuery;
  }
};

initFromQuery();

watch([selectedSymbol, selectedInterval], () => {
  syncRouteQuery();
  fetchChartData();
}, { immediate: true });
</script>



<style scoped>
.select-input {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: var(--ink);
  outline: none;
  transition: all 0.2s ease;
}

.select-input:hover {
  border-color: rgba(148, 163, 184, 0.4);
}

.select-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.chart-wrapper {
  min-height: 500px;
  height: 60vh;
  max-height: 700px;
}

.text-accent {
  color: var(--accent);
}
</style>
