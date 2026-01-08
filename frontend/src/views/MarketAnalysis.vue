<template>
  <MainLayout>
    <div class="analysis-container">
      <div class="chart-panel">
        <CandleStickChart />
      </div>
      <div class="side-panel">
        <div class="parameters section">
          <h3>Parameters</h3>
          <!-- Parameters Form Placeholder -->
          <a-form layout="vertical">
            <a-form-item label="Bi (Pen) Logic">
              <a-checkbox>Strict Inclusion</a-checkbox>
            </a-form-item>
          </a-form>
        </div>
        <div class="signals section">
          <h3>Signals</h3>
          <!-- Signal List Placeholder -->
          <a-list size="small" bordered :data-source="signals">
             <template #renderItem="{ item }">
               <a-list-item>{{ item }}</a-list-item>
             </template>
          </a-list>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { ref } from 'vue';
import MainLayout from '../layouts/MainLayout.vue';
import CandleStickChart from '../components/Chart/CandleStickChart.vue';

const signals = ref(['Buy Signal - 15m - 3rd Buy', 'Sell Signal - 1h - Divergence']);
</script>

<style scoped>
.analysis-container {
  display: flex;
  height: calc(100vh - 64px); /* Subtract header height */
}

.chart-panel {
  flex: 1;
  background: #000;
  border-right: 1px solid #303030;
}

.side-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  background: #1f1f1f;
}

.section {
  padding: 16px;
  border-bottom: 1px solid #303030;
  flex: 1;
}

h3 {
  color: #ddd;
  margin-bottom: 12px;
}
</style>
