<template>
  <section class="flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>策略实验室</h2>
        <div class="panel-sub">分级参数化缠论规则</div>
      </div>
      <div class="strategy-actions flex items-center gap-3">
        <button class="button-ghost">保存模板</button>
        <button class="button">运行回测</button>
      </div>
    </div>
    <div class="strategy-grid grid grid-cols-1 md:grid-cols-3 gap-3">
      <div v-for="param in strategyParams" :key="param.label" class="param p-3">
        <label>{{ param.label }}</label>
        <div class="value">{{ param.value }}</div>
        <input type="range" :min="param.min" :max="param.max" :value="param.value" />
      </div>
    </div>
  </section>
</template>

<script setup>
const strategyParams = [
  { label: '最小成笔K数', value: 5, min: 3, max: 9 },
  { label: '中枢重叠度', value: 62, min: 40, max: 80 },
  { label: '背驰阈值', value: 12, min: 5, max: 25 },
];
</script>
