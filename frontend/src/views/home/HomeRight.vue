<template>
  <section class="signal-card card flex flex-col gap-3 p-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>信号监控</h2>
        <div class="panel-sub">实时队列与置信度</div>
      </div>
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
</template>

<script setup>
const signalFeed = [
  { tag: 'buy', code: 'B1', title: 'BTC/USDT | 1m', meta: '中枢突破 | 0.71', time: '00:48' },
  { tag: 'wait', code: 'S2', title: 'ETH/USDT | 5m', meta: '弱背驰', time: '02:14' },
  { tag: 'sell', code: 'S1', title: 'BNB/USDT | 15m', meta: '线段衰竭', time: '04:02' },
];
</script>
