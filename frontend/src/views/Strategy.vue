<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>策略实验室</h2>
        <div class="panel-sub">模板与回测入口。</div>
      </div>
      <div class="flex gap-2">
        <button class="button-ghost">新建模板</button>
        <button class="button">运行回测</button>
      </div>
    </div>
    <div class="grid gap-3 md:grid-cols-3">
      <div v-for="tpl in strategyTemplates" :key="tpl.name" class="mini-card p-3">
        <div class="panel-sub">{{ tpl.levels }}</div>
        <strong class="mono">{{ tpl.name }}</strong>
        <div class="panel-sub">胜率 {{ tpl.winRate }} | 评级 {{ tpl.rating }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
const strategyTemplates = [
  { name: '多级别共振', levels: '1m/5m/15m', winRate: '54%', rating: 'A-' },
  { name: '背驰反转', levels: '5m/15m', winRate: '51%', rating: 'B+' },
  { name: '趋势延伸', levels: '15m/1h', winRate: '56%', rating: 'A' },
];
</script>
