<template>
  <div ref="chartContainer" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';

const chartContainer = ref(null);
let chartInstance = null;

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value, 'dark');
    
    const option = {
      backgroundColor: '#000000',
      title: {
        text: 'BTC/USDT 1m',
        left: 10
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      grid: {
        left: '3%',
        right: '3%',
        bottom: '10%'
      },
      xAxis: {
        data: ['2023-10-01', '2023-10-02', '2023-10-03', '2023-10-04']
      },
      yAxis: {
        scale: true,
        splitLine: { show: false }
      },
      series: [
        {
          type: 'candlestick',
          data: [
            [20, 34, 10, 38],
            [40, 35, 30, 50],
            [31, 38, 33, 44],
            [38, 15, 5, 42]
          ]
        }
      ]
    };
    chartInstance.setOption(option);
  }
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance?.dispose();
});

const handleResize = () => {
  chartInstance?.resize();
};
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}
</style>
