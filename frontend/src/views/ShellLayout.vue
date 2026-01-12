<template>
  <ChanShell
    :section="section"
    :ticker="tickerView"
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
import { computed, reactive, ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import ChanShell from '../components/ChanShell.vue';
import { useI18n } from '../composables/useI18n';

const route = useRoute();
const { t, locale } = useI18n();

// Reactive ticker data - simulating real-time updates
const ticker = reactive({
  pair: 'BTC/USDT',
  price: '67,420.5',
  change: '+2.41%',
  changeDir: 'up',
  sourceKey: 'shell.binanceSpot',
  interval: '1m',
});

// Model and data source settings
const modelName = ref('Param-A');
const dataSourceKey = ref('shell.binanceSpot');
const dataSource = computed(() => t(dataSourceKey.value));
const syncInterval = ref('1m');
const syncRate = ref('98.7%');

const tickerView = computed(() => ({
  ...ticker,
  source: t(ticker.sourceKey),
}));

const section = computed(() => route.meta.section || 'home');
const hasRight = computed(() => route.meta.hasRight ?? false);
const hasBottom = computed(() => route.meta.hasBottom ?? false);

// Timer reference for cleanup
let tickerTimer = null;

// Simulate real-time ticker updates
onMounted(() => {
  tickerTimer = setInterval(() => {
    // Simulate price changes
    const basePrice = 67420.5;
    const variation = (Math.random() - 0.5) * 100;
    const newPrice = basePrice + variation;
    ticker.price = newPrice.toLocaleString(locale.value, { minimumFractionDigits: 1, maximumFractionDigits: 1 });

    // Simulate change percentage
    const changeVal = (Math.random() - 0.4) * 5;
    ticker.change = `${changeVal >= 0 ? '+' : ''}${changeVal.toFixed(2)}%`;
    ticker.changeDir = changeVal >= 0 ? 'up' : 'down';
  }, 3000);
});

// Clean up timer on component unmount
onBeforeUnmount(() => {
  if (tickerTimer) {
    clearInterval(tickerTimer);
    tickerTimer = null;
  }
});
</script>
