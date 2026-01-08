<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>设置</h2>
        <div class="panel-sub">偏好、风控、账户绑定。</div>
      </div>
      <button class="button">保存设置</button>
    </div>
    <div class="grid gap-3 md:grid-cols-2">
      <div class="mini-card p-3">
        <div class="panel-sub">主题</div>
        <div class="flex gap-2 mt-2">
          <button class="button">暗色</button>
          <button class="button-ghost">浅色</button>
        </div>
      </div>
      <div class="mini-card p-3">
        <div class="panel-sub">风控</div>
        <div class="mono text-lg mt-1">单标的回撤 3%</div>
        <div class="panel-sub">超限自动降权</div>
      </div>
      <div class="mini-card p-3">
        <div class="panel-sub">账户绑定</div>
        <div class="mono mt-1">Binance API · 已连接</div>
        <div class="panel-sub">Key ****ABCD</div>
      </div>
      <div class="mini-card p-3">
        <div class="panel-sub">通知方式</div>
        <div class="flex gap-2 mt-2">
          <button class="select-pill">邮件</button>
          <button class="select-pill">Webhook</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
</script>
