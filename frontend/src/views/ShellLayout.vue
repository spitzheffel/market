<template>
  <ChanShell :section="section" :ticker="ticker" :has-right="hasRight" :has-bottom="hasBottom">
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
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ChanShell from '../components/ChanShell.vue';

const route = useRoute();
const ticker = {
  pair: 'BTC/USDT',
  price: '67,420.5',
  change: '+2.41%',
  changeDir: 'up',
  source: 'Binance 现货',
  interval: '1m',
};

const section = computed(() => route.meta.section || 'home');
const hasRight = computed(() => route.meta.hasRight ?? false);
const hasBottom = computed(() => route.meta.hasBottom ?? false);
</script>
