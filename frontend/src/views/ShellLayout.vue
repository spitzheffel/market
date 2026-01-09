<template>
  <ChanShell
    :section="section"
    :ticker="ticker"
    :model-name="modelName"
    :data-source="dataSource"
    :sync-interval="syncInterval"
    :sync-rate="syncRate"
    :has-right="hasRight"
    :has-bottom="hasBottom"
  >
    <router-view />
    <template #right>
      <router-view name="right" v-if="hasRight" />
    </template>
    <template #bottom>
      <router-view name="bottom" v-if="hasBottom" />
    </template>
  </ChanShell>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import ChanShell from '../components/ChanShell.vue';

const route = useRoute();

// Reactive ticker data - simulating real-time updates
const ticker = reactive({
  pair: 'BTC/USDT',
  price: '67,420.5',
  change: '+2.41%',
  changeDir: 'up',
  source: 'Binance 现货',
  interval: '1m',
});

// Model and data source settings
const modelName = ref('Param-A');
const dataSource = ref('Binance 现货');
const syncInterval = ref('1m');
const syncRate = ref('98.7%');

const section = computed(() => route.meta.section || 'home');
const hasRight = computed(() => route.meta.hasRight ?? false);
const hasBottom = computed(() => route.meta.hasBottom ?? false);

// Simulate real-time ticker updates
onMounted(() => {
  setInterval(() => {
    // Simulate price changes
    const basePrice = 67420.5;
    const variation = (Math.random() - 0.5) * 100;
    const newPrice = basePrice + variation;
    ticker.price = newPrice.toLocaleString('en-US', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

    // Simulate change percentage
    const changeVal = (Math.random() - 0.4) * 5;
    ticker.change = `${changeVal >= 0 ? '+' : ''}${changeVal.toFixed(2)}%`;
    ticker.changeDir = changeVal >= 0 ? 'up' : 'down';
  }, 3000);
});
</script>
