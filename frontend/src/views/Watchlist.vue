<template>
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
        :value="`${summary.count} 标的`"
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
        <tr v-for="row in filteredWatchlist" :key="row.pair">
          <td>
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
          :item-height="130"
          :buffer="2"
          height="calc(100vh - 450px)"
          :item-key="(item) => item.pair"
        >
          <template #default="{ item: row }">
            <div
              class="watchlist-card mini-card p-4 flex flex-col gap-3 active:bg-[rgba(59,130,246,0.08)] transition-colors"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="flex items-start gap-3 flex-1">
                  <input
                    type="checkbox"
                    class="h-4 w-4 accent-[var(--accent)] mt-0.5"
                    :checked="selected.includes(row.pair)"
                    @change="toggleSelect(row.pair)"
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
            class="watchlist-card mini-card p-4 flex flex-col gap-3 active:bg-[rgba(59,130,246,0.08)] transition-colors"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-start gap-3 flex-1">
                <input
                  type="checkbox"
                  class="h-4 w-4 accent-[var(--accent)] mt-0.5"
                  :checked="selected.includes(row.pair)"
                  @change="toggleSelect(row.pair)"
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
import { ref, reactive, computed } from 'vue';
import { useI18n } from '../composables/useI18n';
import CardHeader from '../components/common/CardHeader.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import DataTable from '../components/common/DataTable.vue';
import StatusTag from '../components/common/StatusTag.vue';
import CardSection from '../components/common/CardSection.vue';
import KpiStat from '../components/common/KpiStat.vue';
import VirtualList from '../components/common/VirtualList.vue';

const { t } = useI18n();

const loading = ref(false);
const searchQuery = ref('');
const selected = ref([]);

const filters = reactive({
  group: 'all',
});

const groupOptions = [
  { label: t('common.all'), value: 'all', count: 25 },
  { label: t('watchlist.mainstream'), value: 'mainstream', count: 12 },
  { label: t('watchlist.futures'), value: 'futures', count: 8 },
  { label: t('watchlist.watch'), value: 'watch', count: 5 },
];

const groupSummaries = [
  { name: t('watchlist.mainstream'), count: 12, desc: 'BTC, ETH, SOL...' },
  { name: t('watchlist.futures'), count: 8, desc: '永续/USDT 本位' },
  { name: t('watchlist.watch'), count: 5, desc: '暂不交易' },
];

const watchlist = ref([
  { pair: 'ETH/USDT', tags: [t('watchlist.mainstream'), t('watchlist.spot')], subs: ['1m', '5m'], signal: 'buy', bias: '+1.2%' },
  { pair: 'SOL/USDT', tags: [t('watchlist.mainstream'), t('watchlist.perpetual')], subs: ['5m', '15m'], signal: 'wait', bias: '+0.3%' },
  { pair: 'BNB/USDT', tags: [t('watchlist.mainstream'), t('watchlist.spot')], subs: ['1m'], signal: 'sell', bias: '-0.9%' },
  { pair: 'ARB/USDT', tags: [t('watchlist.watch')], subs: ['15m'], signal: 'wait', bias: '+0.1%' },
]);

// 计算信号标签
const getSignalLabel = (signal) => {
  return t(`signals.${signal}`);
};

// 为每个自选添加信号标签
const watchlistWithLabels = computed(() => {
  return watchlist.value.map(w => ({
    ...w,
    signalLabel: getSignalLabel(w.signal),
  }));
});

const filteredWatchlist = computed(() => {
  let result = watchlistWithLabels.value;

  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter(
      (w) => w.pair.includes(query) || w.tags.some((t) => t.toUpperCase().includes(query))
    );
  }

  if (filters.group !== 'all') {
    const groupMap = {
      mainstream: t('watchlist.mainstream'),
      futures: t('watchlist.perpetual'),
      watch: t('watchlist.watch'),
    };
    const groupTag = groupMap[filters.group];
    if (groupTag) {
      result = result.filter((w) => w.tags.includes(groupTag));
    }
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
</script>
