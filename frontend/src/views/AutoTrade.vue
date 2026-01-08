<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>自动交易</h2>
        <div class="panel-sub">通道状态与风控。</div>
      </div>
      <button class="button">启动</button>
    </div>
    <div class="grid gap-3 md:grid-cols-2">
      <div class="mini-card p-3">
        交易通道
        <strong class="mono">Binance</strong>
        <div class="panel-sub">状态：在线</div>
      </div>
      <div class="mini-card p-3">
        风控阈值
        <strong class="mono">3.0%</strong>
        <div class="panel-sub">单标的回撤</div>
      </div>
    </div>
    <div class="panel-sub">最近委托：暂无</div>
  </section>
</template>

<script setup>
</script>
