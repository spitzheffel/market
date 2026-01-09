<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader title="回测" subtitle="任务与结果概览。">
      <template #actions>
        <button class="button">新建回测</button>
      </template>
    </CardHeader>

    <!-- Loading state -->
    <div v-if="loading" class="grid gap-3">
      <Skeleton v-for="i in 3" :key="i" variant="card" height="100px" />
    </div>

    <!-- Empty state -->
    <EmptyState
      v-else-if="!backtests.length"
      title="暂无回测任务"
      description="点击上方按钮创建新的回测任务"
    >
      <template #action>
        <button class="button">新建回测</button>
      </template>
    </EmptyState>

    <!-- Backtest list -->
    <div v-else class="grid gap-3">
      <div v-for="task in backtests" :key="task.id" class="mini-card p-4">
        <div class="flex justify-between items-start mb-2">
          <div class="mono font-semibold">{{ task.name }}</div>
          <StatusTag
            :variant="getStatusVariant(task.status)"
            :label="task.statusLabel"
            :dot="task.status === 'running'"
          />
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">区间</div>
            <div class="mono text-sm mt-1">{{ task.range }}</div>
          </div>
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">收益</div>
            <div
              class="mono text-sm mt-1"
              :class="task.pnl.startsWith('+') ? 'text-success' : task.pnl.startsWith('-') ? 'text-danger' : 'text-muted'"
            >
              {{ task.pnl }}
            </div>
          </div>
          <div>
            <div class="text-xs text-muted uppercase tracking-wide">回撤</div>
            <div class="mono text-sm mt-1">{{ task.dd }}</div>
          </div>
          <div v-if="task.status === 'running'">
            <div class="text-xs text-muted uppercase tracking-wide">进度</div>
            <div class="mt-2">
              <div class="h-1.5 rounded-full bg-[rgba(148,163,184,0.18)] overflow-hidden">
                <div
                  class="h-full bg-gradient-to-r from-[#38bdf8] to-[#22d3ee] transition-all"
                  :style="{ width: task.progress || '45%' }"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../components/common/CardHeader.vue';
import StatusTag from '../components/common/StatusTag.vue';
import Skeleton from '../components/common/Skeleton.vue';
import EmptyState from '../components/common/EmptyState.vue';

const loading = ref(true);

const backtests = ref([
  { id: 1, name: 'BTC-1m-三买', status: 'done', statusLabel: '完成', range: '2024-01 ~ 2024-03', pnl: '+12.4%', dd: '3.1%' },
  { id: 2, name: 'ETH-5m-突破', status: 'running', statusLabel: '运行中', range: '2024-02 ~ 2024-03', pnl: '+5.2%', dd: '2.4%', progress: '65%' },
  { id: 3, name: 'SOL-15m-回测', status: 'queued', statusLabel: '排队', range: '2023-12 ~ 2024-03', pnl: '--', dd: '--' },
]);

const getStatusVariant = (status) => {
  const map = {
    done: 'success',
    running: 'running',
    queued: 'queued',
    failed: 'danger',
  };
  return map[status] || 'neutral';
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 900));
  loading.value = false;
});
</script>
