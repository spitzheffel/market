<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>回测</h2>
        <div class="panel-sub">任务与结果概览。</div>
      </div>
    <button class="button">新建回测</button>
    </div>
    <div class="grid gap-3">
      <div v-for="task in backtests" :key="task.id" class="mini-card p-3">
        <div class="flex justify-between">
          <div class="mono">{{ task.name }}</div>
          <span :class="['tag', task.status === 'done' ? 'buy' : task.status === 'running' ? 'wait' : 'sell']">
            {{ task.statusLabel }}
          </span>
        </div>
        <div class="panel-sub">区间 {{ task.range }}</div>
        <div class="panel-sub">收益 {{ task.pnl }} | 回撤 {{ task.dd }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
const backtests = [
  { id: 1, name: 'BTC-1m-三买', status: 'done', statusLabel: '完成', range: '2024-01 ~ 2024-03', pnl: '+12.4%', dd: '3.1%' },
  { id: 2, name: 'ETH-5m-突破', status: 'running', statusLabel: '运行中', range: '2024-02 ~ 2024-03', pnl: '+5.2%', dd: '2.4%' },
  { id: 3, name: 'SOL-15m-回测', status: 'queued', statusLabel: '排队', range: '2023-12 ~ 2024-03', pnl: '--', dd: '--' },
];
</script>
