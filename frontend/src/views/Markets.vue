<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('markets.title')" :subtitle="t('markets.subtitle')">
      <template #actions>
        <div class="flex flex-wrap gap-2">
          <div class="flex items-center gap-2">
            <label class="text-xs text-muted">{{ t('markets.dataSource') }}:</label>
            <select v-model="topFilters.exchange" class="top-filter-select">
              <option value="">{{ t('markets.all') }}</option>
              <option v-for="ex in exchangeOptions" :key="ex" :value="ex">
                {{ formatExchangeLabel(ex) }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs text-muted">{{ t('markets.contractType') }}:</label>
            <select v-model="topFilters.marketType" class="top-filter-select">
              <option value="">{{ t('markets.all') }}</option>
              <option value="spot">{{ t('markets.spot') }}</option>
              <option value="futures">{{ t('markets.futures') }}</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs text-muted">{{ t('markets.defaultInterval') }}:</label>
            <select v-model="topFilters.defaultInterval" class="top-filter-select">
              <option value="">{{ t('markets.all') }}</option>
              <option v-for="int in intervalOptions" :key="int" :value="int">{{ int }}</option>
            </select>
          </div>
          <button class="button-ghost" @click="refresh" :disabled="loading">
            {{ loading ? t('common.refreshing') : t('common.refresh') }}
          </button>
        </div>
      </template>
    </CardHeader>

    <div class="flex flex-wrap items-center gap-3">
      <input
        v-model="searchQuery"
        type="text"
        :placeholder="t('markets.searchPlaceholder')"
        class="w-full md:w-64 rounded-full border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-4 py-2 text-sm text-slate-100 placeholder:text-slate-500"
      />
      <FilterGroup :label="t('markets.type')">
        <FilterPill
          v-for="type in typeOptions"
          :key="type.value"
          :label="t(`markets.${type.value}`)"
          :active="filters.type === type.value"
          @click="filters.type = type.value"
        />
      </FilterGroup>
      <FilterGroup :label="t('markets.interval')">
        <FilterPill
          :label="t('markets.all')"
          :active="filters.interval === ''"
          @click="filters.interval = ''"
        />
        <FilterPill
          v-for="interval in intervalOptions"
          :key="interval"
          :label="interval"
          :active="filters.interval === interval"
          @click="filters.interval = interval"
        />
      </FilterGroup>
      <FilterGroup :label="t('common.sort')">
        <FilterPill
          v-for="sort in sortOptions"
          :key="sort.value"
          :label="t(`markets.sortBy${sort.value.charAt(0).toUpperCase() + sort.value.slice(1)}`)"
          :active="filters.sort === sort.value"
          @click="filters.sort = sort.value"
        />
      </FilterGroup>
    </div>

    <div class="grid gap-3 md:grid-cols-3">
      <KpiStat
        :label="t('markets.topMovers')"
        :value="topMovers[0]?.change ? `${topMovers[0].change}%` : ''"
        :note="topMovers[0]?.pair || ''"
        :tone="topMovers[0]?.change?.startsWith('-') ? 'down' : 'up'"
        :loading="loading"
        :empty="!loading && !topMovers.length"
      />
      <KpiStat :label="t('markets.volume24h')" :value="volume24h" note="USDT" :loading="loading" />
      <KpiStat :label="t('markets.scanInterval')" value="5s" :note="t('markets.realTimePush')" />
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !filteredMarkets.length"
      :column-count="7"
      :empty-text="t('markets.emptyText')"
      :card-mode="true"
    >
      <template #header>
        <th>{{ t('markets.pair') }}</th>
        <th class="numeric">{{ t('markets.lastPrice') }}</th>
        <th class="numeric">{{ t('markets.change') }}</th>
        <th class="numeric hide-mobile">{{ t('markets.volume') }}</th>
        <th class="numeric hide-mobile">{{ t('markets.volatility') }}</th>
        <th class="hide-tablet">{{ t('markets.interval') }}</th>
        <th>{{ t('markets.signal') }}</th>
      </template>
      <template #body>
        <tr v-for="row in filteredMarkets" :key="row.pair" class="cursor-pointer hover:bg-[rgba(59,130,246,0.05)]" @click="goToChart(row.pair)">
          <td>{{ row.pair }}</td>
          <td class="numeric">{{ row.last }}</td>
          <td :class="['numeric', row.change.startsWith('-') ? 'text-danger' : 'text-success']">{{ row.change }}%</td>
          <td class="numeric hide-mobile">{{ row.volume }}</td>
          <td class="numeric hide-mobile">{{ row.volatility }}</td>
          <td class="hide-tablet">{{ row.interval }}</td>
          <td><StatusTag :variant="row.signal" :label="row.signalLabel" /></td>
        </tr>
      </template>
      <template #cards>
        <VirtualList
          v-if="filteredMarkets.length > 20"
          :items="filteredMarkets"
          :item-height="150"
          :buffer="2"
          height="calc(100vh - 400px)"
          :item-key="(item) => item.pair"
        >
          <template #default="{ item: row }">
            <div
              class="market-card mini-card p-4 flex flex-col gap-3 cursor-pointer hover:bg-[rgba(59,130,246,0.08)] active:bg-[rgba(59,130,246,0.12)] transition-colors"
              @click="goToChart(row.pair)"
            >
              <div class="flex items-start justify-between">
                <div class="flex flex-col gap-1">
                  <span class="font-semibold text-sm">{{ row.pair }}</span>
                  <span class="text-xs text-muted">{{ row.interval }} · {{ row.volume }}</span>
                </div>
                <StatusTag :variant="row.signal" :label="row.signalLabel" size="sm" />
              </div>
              <div class="flex items-center justify-between">
                <span class="mono text-lg font-semibold">{{ row.last }}</span>
                <span :class="['mono text-sm font-semibold', row.change.startsWith('-') ? 'text-danger' : 'text-success']">
                  {{ row.change }}%
                </span>
              </div>
            </div>
          </template>
        </VirtualList>
        <template v-else>
          <div
            v-for="row in filteredMarkets"
            :key="row.pair"
            class="market-card mini-card p-4 flex flex-col gap-3 cursor-pointer hover:bg-[rgba(59,130,246,0.08)] active:bg-[rgba(59,130,246,0.12)] transition-colors"
            @click="goToChart(row.pair)"
          >
            <div class="flex items-start justify-between">
              <div class="flex flex-col gap-1">
                <span class="font-semibold text-sm">{{ row.pair }}</span>
                <span class="text-xs text-muted">{{ row.interval }} · {{ row.volume }}</span>
              </div>
              <StatusTag :variant="row.signal" :label="row.signalLabel" size="sm" />
            </div>
            <div class="flex items-center justify-between">
              <span class="mono text-lg font-semibold">{{ row.last }}</span>
              <span :class="['mono text-sm font-semibold', row.change.startsWith('-') ? 'text-danger' : 'text-success']">
                {{ row.change }}%
              </span>
            </div>
          </div>
        </template>
      </template>
    </DataTable>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import KpiStat from '../components/common/KpiStat.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import DataTable from '../components/common/DataTable.vue';
import StatusTag from '../components/common/StatusTag.vue';
import VirtualList from '../components/common/VirtualList.vue';
import { klineApi } from '../api/market';

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const searchQuery = ref('');

// 顶部筛选器
// 注意：当前后端仅编译启用 Binance，OKX/Bybit 待后续启用
const exchangeOptions = ref(['binance']);
const defaultExchange = ref('binance');
const marketSymbols = [
  { pair: 'BTC/USDT', type: 'spot' },
  { pair: 'ETH/USDT', type: 'spot' },
  { pair: 'SOL/USDT', type: 'spot' },
  { pair: 'BNB/USDT', type: 'spot' },
  { pair: 'XRP/USDT', type: 'spot' },
  { pair: 'DOGE/USDT', type: 'spot' },
];
const topFilters = reactive({
  exchange: '',
  marketType: '',
  defaultInterval: ''
});

const filters = reactive({
  type: 'all',
  interval: '',
  sort: 'change',
});

const typeOptions = [
  { value: 'all' },
  { value: 'spot' },
  { value: 'futures' },
];

const intervalOptions = ['1m', '5m', '15m'];

const sortOptions = [
  { value: 'change' },
  { value: 'volume' },
  { value: 'volatility' },
];

const markets = ref([]);
let marketRequestId = 0;

const formatExchangeLabel = (value) => {
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1);
};

const toNumber = (value) => {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const formatPrice = (value) => {
  if (!Number.isFinite(value)) return '--';
  const digits = value >= 100 ? 2 : value >= 1 ? 4 : 6;
  return new Intl.NumberFormat(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(value);
};

const formatPercent = (value) => {
  if (!Number.isFinite(value)) return '0.00';
  const sign = value > 0 ? '+' : '';
  return `${sign}${value.toFixed(2)}`;
};

const formatVolume = (value) => {
  if (!Number.isFinite(value) || value <= 0) return '--';
  if (value >= 1e9) return `${(value / 1e9).toFixed(2)}B`;
  if (value >= 1e6) return `${(value / 1e6).toFixed(2)}M`;
  return `${value.toFixed(0)}`;
};

const formatVolatility = (high, low, last) => {
  if (!Number.isFinite(high) || !Number.isFinite(low) || !Number.isFinite(last) || last === 0) {
    return '--';
  }
  return `${(((high - low) / last) * 100).toFixed(2)}%`;
};

const resolveSignal = (changePercent) => {
  if (changePercent >= 1.2) return 'buy';
  if (changePercent <= -1.2) return 'sell';
  return 'wait';
};

const resolveExchange = () => {
  return topFilters.exchange || defaultExchange.value || 'binance';
};

const resolveRowInterval = () => {
  return topFilters.defaultInterval || '1m';
};

const buildMarketRow = (item, ticker, exchange, interval) => {
  const last = ticker ? toNumber(ticker.lastPrice) : 0;
  const changePercent = ticker ? toNumber(ticker.priceChangePercent) : 0;
  const high = ticker ? toNumber(ticker.high) : 0;
  const low = ticker ? toNumber(ticker.low) : 0;
  const baseVolume = ticker ? toNumber(ticker.volume) : 0;
  const quoteVolume = last > 0 ? baseVolume * last : baseVolume;

  return {
    pair: item.pair,
    last: formatPrice(last),
    change: formatPercent(changePercent),
    volume: formatVolume(quoteVolume),
    volatility: formatVolatility(high, low, last),
    interval,
    signal: resolveSignal(changePercent),
    type: item.type,
    exchange
  };
};

const loadExchanges = async () => {
  try {
    const response = await klineApi.getExchanges();
    const available = response?.available || [];
    if (available.length > 0) {
      exchangeOptions.value = available;
    }
    if (response?.defaultExchange) {
      defaultExchange.value = response.defaultExchange;
    }
  } catch (error) {
    console.warn('Failed to load exchanges', error);
  }

  if (!exchangeOptions.value.length) {
    exchangeOptions.value = ['binance'];
  }
};

const fetchMarkets = async () => {
  const requestId = ++marketRequestId;
  loading.value = true;
  const exchange = resolveExchange();
  const interval = resolveRowInterval();

  try {
    const results = await Promise.all(
      marketSymbols.map(async (item) => {
        try {
          const ticker = await klineApi.getTicker(item.pair, exchange);
          return { item, ticker };
        } catch (error) {
          console.warn('Failed to load ticker', item.pair, error);
          return { item, ticker: null };
        }
      })
    );

    if (requestId !== marketRequestId) return;
    markets.value = results.map(({ item, ticker }) =>
      buildMarketRow(item, ticker, exchange, interval)
    );
  } catch (error) {
    if (requestId !== marketRequestId) return;
    console.error('Failed to load markets', error);
    markets.value = [];
  } finally {
    if (requestId === marketRequestId) {
      loading.value = false;
    }
  }
};

const normalizeTypeFilter = (value) => value || 'all';
const normalizeTypeSelect = (value) => (value === 'all' ? '' : value);
const normalizeInterval = (value) => value || '';

watch(() => topFilters.marketType, (value) => {
  const mapped = normalizeTypeFilter(value);
  if (filters.type !== mapped) {
    filters.type = mapped;
  }
});

watch(() => filters.type, (value) => {
  const mapped = normalizeTypeSelect(value);
  if (topFilters.marketType !== mapped) {
    topFilters.marketType = mapped;
  }
});

watch(() => topFilters.defaultInterval, (value) => {
  const mapped = normalizeInterval(value);
  if (filters.interval !== mapped) {
    filters.interval = mapped;
  }
  const resolved = value || '1m';
  if (markets.value.length) {
    markets.value = markets.value.map((item) => ({
      ...item,
      interval: resolved
    }));
  }
});

watch(() => filters.interval, (value) => {
  const mapped = normalizeInterval(value);
  if (topFilters.defaultInterval !== mapped) {
    topFilters.defaultInterval = mapped;
  }
});

watch(() => topFilters.exchange, () => {
  fetchMarkets();
});

// 计算信号标签
const getSignalLabel = (signal) => {
  return t(`signals.${signal}`);
};

// 为每个市场添加信号标签
const marketsWithLabels = computed(() => {
  return markets.value.map(m => ({
    ...m,
    signalLabel: getSignalLabel(m.signal),
  }));
});

// 解析数值用于排序
const parseValue = (value) => {
  if (typeof value === 'number') return value;
  const str = String(value).replace(/[^0-9.-]/g, '');
  const num = parseFloat(str);
  if (!Number.isFinite(num)) return 0;
  // 处理 B/M 单位
  if (String(value).includes('B')) return num * 1000;
  if (String(value).includes('M')) return num;
  return num;
};

const filteredMarkets = computed(() => {
  let result = marketsWithLabels.value;

  // 顶部筛选器：交易所过滤
  if (topFilters.exchange) {
    result = result.filter((m) => m.exchange === topFilters.exchange);
  }

  // 顶部筛选器：合约类型过滤
  if (topFilters.marketType) {
    result = result.filter((m) => m.type === topFilters.marketType);
  }

  // 顶部筛选器：默认周期过滤
  if (topFilters.defaultInterval) {
    result = result.filter((m) => m.interval === topFilters.defaultInterval);
  }

  // 类型过滤（下方筛选器）
  if (filters.type && filters.type !== 'all') {
    result = result.filter((m) => m.type === filters.type);
  }

  // 周期过滤（下方筛选器）
  if (filters.interval && filters.interval !== '') {
    result = result.filter((m) => m.interval === filters.interval);
  }

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter((m) => m.pair.includes(query));
  }

  // 排序
  if (filters.sort) {
    result = [...result].sort((a, b) => {
      const aVal = parseValue(a[filters.sort]);
      const bVal = parseValue(b[filters.sort]);
      return bVal - aVal; // 降序
    });
  }

  return result;
});

const topMovers = computed(() => {
  const sorted = [...filteredMarkets.value].sort((a, b) => parseValue(b.change) - parseValue(a.change));
  return sorted.slice(0, 1);
});

const volume24h = computed(() => {
  const total = filteredMarkets.value.reduce((sum, m) => sum + parseValue(m.volume), 0);
  return total >= 1000 ? `${(total / 1000).toFixed(2)}B` : `${total.toFixed(0)}M`;
});

const refresh = async () => {
  await fetchMarkets();
};

const resolveChartInterval = (row) => {
  if (filters.interval) return filters.interval;
  if (topFilters.defaultInterval) return topFilters.defaultInterval;
  return row.interval;
};

const goToChart = (symbol) => {
  const row = filteredMarkets.value.find((item) => item.pair === symbol);
  const interval = row ? resolveChartInterval(row) : filters.interval || topFilters.defaultInterval || '1m';
  router.push({
    path: '/chart',
    query: { symbol, interval }
  });
};

onMounted(async () => {
  await loadExchanges();
  await fetchMarkets();
});
</script>
