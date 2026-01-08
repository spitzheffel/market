<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>缠论引擎</h2>
        <div class="panel-sub">规则参数与级别同步。</div>
      </div>
      <button class="button">保存参数</button>
    </div>
    <div class="grid gap-3 md:grid-cols-3">
      <div v-for="param in engineParams" :key="param.label" class="param p-3">
        <label>{{ param.label }}</label>
        <div class="value">{{ param.value }}</div>
        <input type="range" :min="param.min" :max="param.max" :value="param.value" />
      </div>
    </div>
    <div class="mini-card p-3">
      <div class="panel-sub">说明</div>
      <div class="text-muted text-sm">参数保存后应用于实时计算与回放。</div>
    </div>
  </section>
</template>

<script setup>
const engineParams = [
  { label: '包含关系', value: 1, min: 0, max: 2 },
  { label: '分型最少K数', value: 3, min: 2, max: 6 },
  { label: '线段最少笔数', value: 3, min: 3, max: 6 },
];
</script>
