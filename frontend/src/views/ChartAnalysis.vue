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
      <ChanChart
        v-if="currentChartData"
        :chart-data="currentChartData"
        :show-fenxing="showOptions.fenxing"
        :show-bi="showOptions.bi"
        :show-xianduan="showOptions.xianduan"
        :show-zhongshu="showOptions.zhongshu"
        :show-trading-points="showOptions.tradingPoints"
      />
      <div v-else class="flex items-center justify-center h-full text-muted">
        {{ t('chart.loading') }}
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
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import FilterPill from '../components/common/FilterPill.vue';
import ChanChart from '../components/chart/ChanChart.vue';
import { getChartData } from '../mock/chartData';

const { t } = useI18n();
const route = useRoute();

// 可选标的
const symbols = ['BTC/USDT', 'ETH/USDT', 'SOL/USDT', 'BNB/USDT'];
const selectedSymbol = ref('BTC/USDT');

// 从 URL 参数初始化标的
onMounted(() => {
  const symbolFromQuery = route.query.symbol;
  if (symbolFromQuery && symbols.includes(symbolFromQuery)) {
    selectedSymbol.value = symbolFromQuery;
  }
});

// 可选周期
const intervals = ['1m', '5m', '15m', '1h', '4h', '1d'];
const selectedInterval = ref('1m');

// 显示选项
const showOptions = ref({
  fenxing: true,
  bi: true,
  xianduan: true,
  zhongshu: true,
  tradingPoints: true
});

// 当前图表数据
const currentChartData = computed(() => {
  return getChartData(selectedSymbol.value);
});

// 监听标的变化
watch(selectedSymbol, (newSymbol) => {
  console.log('Symbol changed to:', newSymbol);
});

// 监听周期变化
watch(selectedInterval, (newInterval) => {
  console.log('Interval changed to:', newInterval);
  // 在实际应用中，这里会重新获取对应周期的数据
});
</script>

<style scoped>
.select-input {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: var(--text);
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
