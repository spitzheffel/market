<template>
  <!--
    ⚠️ PLACEHOLDER/EXAMPLE CODE - NOT PART OF PHASE 2 ⚠️

    This page demonstrates UI patterns for watchlist management but does NOT implement
    real Chan Theory signal generation. Current implementation uses price change
    percentages as mock signals.

    Phase 2 Scope: Chan calculation + chart visualization only
    Phase 3 Scope: Real signal generation, watchlist alerts, multi-symbol monitoring

    TODO Phase 3:
    - Integrate with backend /api/chan/trading-points endpoint for real signals
    - Replace mock signal generation with real Chan Theory signals
    - Implement batch symbol monitoring and alert system
    - Add watchlist group management and persistence
    - Optimize API calls with batch ticker endpoint
  -->
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader :title="t('watchlist.title')" :subtitle="t('watchlist.subtitle')">
      <template #actions>
        <div class="flex gap-2">
          <button class="button-ghost">{{ t('watchlist.addGroup') }}</button>
          <button class="button">{{ t('watchlist.batchSubscribe') }}</button>
        </div>
      </template>
    </CardHeader>

    <div class="flex flex-wrap items-center gap-3">
      <FilterGroup>
        <FilterPill
          v-for="group in groupOptions"
          :key="group.value"
          :label="group.label"
          :count="group.count"
          :active="filters.group === group.value"
          @click="filters.group = group.value"
        />
      </FilterGroup>
      <input
        v-model="searchQuery"
        type="text"
        :placeholder="t('watchlist.searchPlaceholder')"
        class="w-full md:w-56 rounded-full border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-4 py-2 text-sm text-slate-100 placeholder:text-slate-500"
      />
    </div>

    <div class="flex flex-wrap items-center gap-2 text-sm">
      <span class="text-muted">{{ t('watchlist.selected') }} {{ selectedCount }}</span>
      <button class="button-ghost" :disabled="!selectedCount">{{ t('watchlist.unsubscribe') }}</button>
      <button class="button-ghost" :disabled="!selectedCount">{{ t('watchlist.removeFromGroup') }}</button>
      <button class="button-ghost" :disabled="!selectedCount">{{ t('common.export') }}</button>
    </div>

    <CardSection :title="t('watchlist.groupOverview')" layout="grid-3" gap="md">
      <KpiStat
        v-for="summary in groupSummaries"
        :key="summary.name"
        :label="summary.name"
        :value="`${summary.count} ${t('watchlist.items')}`"
        :note="summary.desc"
      />
    </CardSection>

    <DataTable
      :loading="loading"
      :empty="!loading && !filteredWatchlist.length"
      :column-count="6"
      :empty-text="t('watchlist.emptyText')"
      :card-mode="true"
    >
      <template #header>
        <th class="w-10">
          <input
            type="checkbox"
            class="h-4 w-4 accent-[var(--accent)]"
            :checked="allSelected"
            @change="toggleSelectAll"
          />
        </th>
        <th>{{ t('watchlist.pair') }}</th>
        <th class="hide-mobile">{{ t('watchlist.tags') }}</th>
        <th class="hide-tablet">{{ t('watchlist.subscriptions') }}</th>
        <th>{{ t('watchlist.signal') }}</th>
        <th class="numeric">{{ t('watchlist.bias') }}</th>
      </template>
      <template #body>
        <tr v-for="row in filteredWatchlist" :key="row.pair" class="cursor-pointer hover:bg-[rgba(59,130,246,0.05)]" @click="goToChart(row.pair)">
          <td @click.stop>
            <input
              type="checkbox"
              class="h-4 w-4 accent-[var(--accent)]"
              :checked="selected.includes(row.pair)"
              @change="toggleSelect(row.pair)"
            />
          </td>
          <td>{{ row.pair }}</td>
          <td class="hide-mobile">
            <div class="flex flex-wrap gap-1">
              <StatusTag
                v-for="tag in row.tags"
                :key="tag"
                :label="tag"
                variant="neutral"
                size="sm"
              />
            </div>
          </td>
          <td class="mono hide-tablet">{{ row.subs.join(' / ') }}</td>
          <td><StatusTag :variant="row.signal" :label="row.signalLabel" /></td>
          <td :class="['numeric', row.bias.startsWith('-') ? 'text-danger' : 'text-success']">{{ row.bias }}</td>
        </tr>
      </template>
      <template #cards>
        <VirtualList
          v-if="filteredWatchlist.length > 20"
          :items="filteredWatchlist"
          :item-height="170"
          :buffer="2"
          height="calc(100vh - 450px)"
          :item-key="(item) => item.pair"
        >
          <template #default="{ item: row }">
            <div
              class="watchlist-card mini-card p-4 flex flex-col gap-3 cursor-pointer hover:bg-[rgba(59,130,246,0.08)] active:bg-[rgba(59,130,246,0.12)] transition-colors"
              @click="goToChart(row.pair)"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="flex items-start gap-3 flex-1">
                  <input
                    type="checkbox"
                    class="h-4 w-4 accent-[var(--accent)] mt-0.5"
                    :checked="selected.includes(row.pair)"
                    @change.stop="toggleSelect(row.pair)"
                  />
                  <div class="flex flex-col gap-1 flex-1">
                    <span class="font-semibold text-sm">{{ row.pair }}</span>
                    <div class="flex flex-wrap gap-1">
                      <StatusTag
                        v-for="tag in row.tags"
                        :key="tag"
                        :label="tag"
                        variant="neutral"
                        size="sm"
                      />
                    </div>
                    <span class="text-xs text-muted mono">{{ row.subs.join(' / ') }}</span>
                  </div>
                </div>
                <StatusTag :variant="row.signal" :label="row.signalLabel" size="sm" />
              </div>
              <div class="flex items-center justify-end">
                <span :class="['mono text-sm font-semibold', row.bias.startsWith('-') ? 'text-danger' : 'text-success']">
                  {{ row.bias }}
                </span>
              </div>
            </div>
          </template>
        </VirtualList>
        <template v-else>
          <div
            v-for="row in filteredWatchlist"
            :key="row.pair"
            class="watchlist-card mini-card p-4 flex flex-col gap-3 cursor-pointer hover:bg-[rgba(59,130,246,0.08)] active:bg-[rgba(59,130,246,0.12)] transition-colors"
            @click="goToChart(row.pair)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-start gap-3 flex-1">
                <input
                  type="checkbox"
                  class="h-4 w-4 accent-[var(--accent)] mt-0.5"
                  :checked="selected.includes(row.pair)"
                  @change.stop="toggleSelect(row.pair)"
                />
                <div class="flex flex-col gap-1 flex-1">
                  <span class="font-semibold text-sm">{{ row.pair }}</span>
                  <div class="flex flex-wrap gap-1">
                    <StatusTag
                      v-for="tag in row.tags"
                      :key="tag"
                      :label="tag"
                      variant="neutral"
                      size="sm"
                    />
                  </div>
                  <span class="text-xs text-muted mono">{{ row.subs.join(' / ') }}</span>
                </div>
              </div>
              <StatusTag :variant="row.signal" :label="row.signalLabel" size="sm" />
            </div>
            <div class="flex items-center justify-end">
              <span :class="['mono text-sm font-semibold', row.bias.startsWith('-') ? 'text-danger' : 'text-success']">
                {{ row.bias }}
              </span>
            </div>
          </div>
        </template>
      </template>
    </DataTable>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import DataTable from '../components/common/DataTable.vue';
import StatusTag from '../components/common/StatusTag.vue';
import CardSection from '../components/common/CardSection.vue';
import KpiStat from '../components/common/KpiStat.vue';
import VirtualList from '../components/common/VirtualList.vue';
import { klineApi, marketCatalogApi } from '../api/market';

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const searchQuery = ref('');
const selected = ref([]);

const filters = reactive({
  group: 'all',
});

const watchlistRaw = ref([]);

const fallbackSymbols = [
  { symbol: 'BTC/USDT', type: 'spot' },
  { symbol: 'ETH/USDT', type: 'spot' },
  { symbol: 'SOL/USDT', type: 'spot' },
  { symbol: 'BNB/USDT', type: 'spot' },
  { symbol: 'ADA/USDT', type: 'spot' },
  { symbol: 'ARB/USDT', type: 'spot' },
];

const mainstreamSymbols = new Set(['BTC/USDT', 'ETH/USDT', 'SOL/USDT', 'BNB/USDT']);

const normalizeType = (type) => {
  if (!type) return 'spot';
  const lower = type.toLowerCase();
  if (lower.includes('perp') || lower.includes('future')) return 'perpetual';
  return lower.includes('spot') ? 'spot' : 'spot';
};

const resolveTagKeys = (symbol, typeKey) => {
  const tags = [];
  if (mainstreamSymbols.has(symbol)) {
    tags.push('mainstream');
  } else {
    tags.push('watch');
  }
  tags.push(typeKey);
  return tags;
};

const buildSubscriptions = (tagKeys, typeKey) => {
  if (typeKey === 'perpetual') return ['1m', '5m', '15m'];
  if (tagKeys.includes('mainstream')) return ['1m', '5m'];
  return ['15m'];
};

const formatBias = (changePercent) => {
  const safeChange = Number.isNaN(changePercent) ? 0 : changePercent;
  const sign = safeChange > 0 ? '+' : '';
  return `${sign}${safeChange.toFixed(2)}%`;
};

const getSignalFromChange = (changePercent) => {
  if (changePercent >= 0.7) return 'buy';
  if (changePercent <= -0.7) return 'sell';
  return 'wait';
};

const buildWatchlistItem = (item, ticker) => {
  const typeKey = normalizeType(item.type);
  const tagKeys = resolveTagKeys(item.symbol, typeKey);
  const changePercent = Number.parseFloat(ticker.priceChangePercent || '0');
  const safeChange = Number.isNaN(changePercent) ? 0 : changePercent;

  return {
    pair: item.symbol,
    tagKeys,
    subs: buildSubscriptions(tagKeys, typeKey),
    signal: getSignalFromChange(safeChange),
    bias: formatBias(safeChange),
  };
};

const groupCounts = computed(() => {
  const counts = {
    total: watchlistRaw.value.length,
    mainstream: 0,
    perpetual: 0,
    watch: 0,
  };

  watchlistRaw.value.forEach((item) => {
    if (item.tagKeys.includes('mainstream')) counts.mainstream += 1;
    if (item.tagKeys.includes('perpetual')) counts.perpetual += 1;
    if (item.tagKeys.includes('watch')) counts.watch += 1;
  });

  return counts;
});

const groupOptions = computed(() => [
  { label: t('common.all'), value: 'all', count: groupCounts.value.total },
  { label: t('watchlist.mainstream'), value: 'mainstream', count: groupCounts.value.mainstream },
  { label: t('watchlist.futures'), value: 'futures', count: groupCounts.value.perpetual },
  { label: t('watchlist.watch'), value: 'watch', count: groupCounts.value.watch },
]);

const groupSummaries = computed(() => [
  { name: t('watchlist.mainstream'), count: groupCounts.value.mainstream, desc: t('watchlist.mainstreamDesc') },
  { name: t('watchlist.futures'), count: groupCounts.value.perpetual, desc: t('watchlist.futuresDesc') },
  { name: t('watchlist.watch'), count: groupCounts.value.watch, desc: t('watchlist.watchDesc') },
]);

const watchlist = computed(() =>
  watchlistRaw.value.map((item) => ({
    ...item,
    tags: item.tagKeys.map((key) => t(`watchlist.${key}`)),
  }))
);

const getSignalLabel = (signal) => {
  return t(`signals.${signal}`);
};

const watchlistWithLabels = computed(() => {
  return watchlist.value.map((item) => ({
    ...item,
    signalLabel: getSignalLabel(item.signal),
  }));
});

const loadWatchlist = async () => {
  loading.value = true;
  try {
    const symbols = await marketCatalogApi.getSymbols();
    const symbolList = symbols.length ? symbols : fallbackSymbols;

    const items = await Promise.all(
      symbolList.map(async (item) => {
        try {
          const ticker = await klineApi.getTicker(item.symbol);
          return buildWatchlistItem(item, ticker);
        } catch (error) {
          console.warn('Failed to load ticker for', item.symbol, error);
          return null;
        }
      })
    );

    watchlistRaw.value = items.filter(Boolean);
    selected.value = selected.value.filter((pair) => watchlistRaw.value.some((item) => item.pair === pair));
  } catch (error) {
    console.error('Failed to load watchlist', error);
    watchlistRaw.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(loadWatchlist);

const filteredWatchlist = computed(() => {
  let result = watchlistWithLabels.value;

  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter(
      (w) =>
        w.pair.toUpperCase().includes(query) ||
        w.tags.some((tag) => tag.toUpperCase().includes(query)) ||
        w.tagKeys.some((tag) => tag.toUpperCase().includes(query))
    );
  }

  if (filters.group !== 'all') {
    const groupKey = filters.group === 'futures' ? 'perpetual' : filters.group;
    result = result.filter((w) => w.tagKeys.includes(groupKey));
  }

  return result;
});

const selectedCount = computed(() => selected.value.length);

const allSelected = computed(() => {
  return filteredWatchlist.value.length > 0 && filteredWatchlist.value.every((w) => selected.value.includes(w.pair));
});

const toggleSelect = (pair) => {
  const idx = selected.value.indexOf(pair);
  if (idx > -1) {
    selected.value.splice(idx, 1);
  } else {
    selected.value.push(pair);
  }
};

const toggleSelectAll = () => {
  if (allSelected.value) {
    selected.value = [];
  } else {
    selected.value = filteredWatchlist.value.map((w) => w.pair);
  }
};

const goToChart = (symbol) => {
  router.push({
    path: '/chart',
    query: { symbol }
  });
};
</script>
