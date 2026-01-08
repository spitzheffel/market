<template>
  <section class="card p-5 flex flex-col gap-4">
    <div class="panel-header flex items-center justify-between gap-4">
      <div>
        <h2>自选池</h2>
        <div class="panel-sub">分组、标签、批量订阅。</div>
      </div>
      <div class="flex gap-2">
        <button class="button-ghost">新增分组</button>
        <button class="button">批量订阅</button>
      </div>
    </div>
    <div class="grid gap-3 md:grid-cols-3">
      <div class="level-item">
        <h4>主流</h4>
        <strong>12 标的</strong>
        <div class="panel-sub">BTC, ETH, SOL...</div>
      </div>
      <div class="level-item">
        <h4>合约</h4>
        <strong>8 标的</strong>
        <div class="panel-sub">永续/USDT 本位</div>
      </div>
      <div class="level-item">
        <h4>观察</h4>
        <strong>5 标的</strong>
        <div class="panel-sub">暂不交易</div>
      </div>
    </div>
    <div class="overflow-x-auto">
      <table>
        <thead>
          <tr>
            <th>交易对</th>
            <th>标签</th>
            <th>信号</th>
            <th>偏向</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in watchlist" :key="row.pair">
            <td>{{ row.pair }}</td>
            <td class="mono">{{ row.tags.join(', ') }}</td>
            <td><span :class="['tag', row.signal]">{{ row.signalLabel }}</span></td>
            <td class="mono">{{ row.bias }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
const watchlist = [
  { pair: 'ETH/USDT', tags: ['主流', '现货'], signal: 'buy', signalLabel: '买', bias: '+1.2%' },
  { pair: 'SOL/USDT', tags: ['主流', '永续'], signal: 'wait', signalLabel: '观望', bias: '+0.3%' },
  { pair: 'BNB/USDT', tags: ['主流', '现货'], signal: 'sell', signalLabel: '卖', bias: '-0.9%' },
  { pair: 'ARB/USDT', tags: ['观察'], signal: 'wait', signalLabel: '观望', bias: '+0.1%' },
];
</script>
