<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader title="信号" subtitle="筛选方向 / 级别 / 标的。">
      <template #actions>
        <div class="flex gap-2">
          <FilterPill
            v-for="dir in directionOptions"
            :key="dir.value"
            :label="dir.label"
            :active="filters.direction === dir.value"
            @click="filters.direction = dir.value"
          />
        </div>
      </template>
    </CardHeader>

    <div class="flex flex-wrap items-center gap-3">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索标的或信号类型"
        class="w-full md:w-64 rounded-full border border-[rgba(148,163,184,0.25)] bg-[rgba(15,23,42,0.7)] px-4 py-2 text-sm text-slate-100 placeholder:text-slate-500"
      />
      <FilterGroup label="级别">
        <FilterPill
          v-for="level in levelOptions"
          :key="level"
          :label="level"
          :active="filters.level === level"
          @click="filters.level = level"
        />
      </FilterGroup>
      <FilterGroup label="置信度">
        <FilterPill
          v-for="conf in confidenceOptions"
          :key="conf.value"
          :label="conf.label"
          :active="filters.confidence === conf.value"
          @click="filters.confidence = conf.value"
        />
      </FilterGroup>
    </div>

    <!-- Signal list with loading/empty states -->
    <div v-if="loading" class="grid gap-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="80px" />
    </div>

    <EmptyState
      v-else-if="!filteredSignals.length"
      title="暂无信号"
      description="当前筛选条件下没有匹配的信号"
      size="sm"
    />

    <div v-else class="grid gap-3">
      <div
        v-for="signal in filteredSignals"
        :key="signal.code"
        class="signal-item grid grid-cols-[auto,1fr,auto] md:grid-cols-[auto,1fr,auto] items-center gap-3 p-3"
      >
        <StatusTag :variant="signal.tag" :label="signal.code" />
        <div>
          <div class="signal-title flex items-center gap-2 flex-wrap">
            <span>{{ signal.title }}</span>
            <StatusTag
              :variant="signal.priority === '高' ? 'danger' : signal.priority === '中' ? 'warning' : 'neutral'"
              :label="signal.priority"
              size="sm"
            />
          </div>
          <div class="signal-meta">{{ signal.meta }}</div>
        </div>
        <div class="flex flex-col items-end gap-1 text-right">
          <div class="mono text-sm">{{ signal.time }}</div>
          <div class="text-xs text-muted hidden md:block">置信度 {{ signal.confidence }}</div>
          <div class="text-xs text-muted hidden md:block">有效期 {{ signal.expires }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import CardHeader from '../components/common/CardHeader.vue';
import FilterPill from '../components/common/FilterPill.vue';
import FilterGroup from '../components/common/FilterGroup.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';

const loading = ref(false);
const searchQuery = ref('');

const filters = reactive({
  direction: 'all',
  level: '1m',
  confidence: 'all',
});

const directionOptions = [
  { label: '全部', value: 'all' },
  { label: '买', value: 'buy' },
  { label: '卖', value: 'sell' },
];

const levelOptions = ['1m', '5m', '15m'];

const confidenceOptions = [
  { label: '>0.7', value: '0.7' },
  { label: '>0.5', value: '0.5' },
  { label: '全部', value: 'all' },
];

const signalFeed = ref([
  { tag: 'buy', code: 'B1', title: 'BTC/USDT | 1m', meta: '中枢突破', time: '00:48', priority: '高', confidence: '0.71', expires: '4m', level: '1m' },
  { tag: 'wait', code: 'S2', title: 'ETH/USDT | 5m', meta: '弱背驰', time: '02:14', priority: '中', confidence: '0.56', expires: '12m', level: '5m' },
  { tag: 'sell', code: 'S1', title: 'BNB/USDT | 15m', meta: '线段衰竭', time: '04:02', priority: '高', confidence: '0.68', expires: '28m', level: '15m' },
  { tag: 'buy', code: 'B2', title: 'SOL/USDT | 1m', meta: '笔突破', time: '01:15', priority: '中', confidence: '0.62', expires: '3m', level: '1m' },
  { tag: 'sell', code: 'S3', title: 'ADA/USDT | 5m', meta: '背驰卖点', time: '03:20', priority: '高', confidence: '0.75', expires: '10m', level: '5m' },
]);

const filteredSignals = computed(() => {
  let result = signalFeed.value;

  // 方向过滤
  if (filters.direction !== 'all') {
    result = result.filter((s) => s.tag === filters.direction);
  }

  // 级别过滤
  if (filters.level) {
    result = result.filter((s) => s.level === filters.level);
  }

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toUpperCase();
    result = result.filter((s) => s.title.toUpperCase().includes(query) || s.meta.includes(query));
  }

  // 置信度过滤
  if (filters.confidence !== 'all') {
    const minConf = parseFloat(filters.confidence);
    result = result.filter((s) => parseFloat(s.confidence) >= minConf);
  }

  return result;
});
</script>
