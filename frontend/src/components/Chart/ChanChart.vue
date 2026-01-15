<template>
  <div class="chan-chart-container">
    <v-chart
      ref="chartRef"
      class="chart"
      :option="chartOption"
      autoresize
      @click="handleChartClick"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { CandlestickChart, LineChart, BarChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  MarkPointComponent,
  MarkLineComponent,
  LegendComponent
} from 'echarts/components';
import VChart from 'vue-echarts';
import { useI18n } from '../../composables/useI18n';

// 注册ECharts组件
use([
  CanvasRenderer,
  CandlestickChart,
  LineChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DataZoomComponent,
  MarkPointComponent,
  MarkLineComponent,
  LegendComponent
]);

const props = defineProps({
  chartData: {
    type: Object,
    required: true
  },
  showFenxing: {
    type: Boolean,
    default: true
  },
  showBi: {
    type: Boolean,
    default: true
  },
  showXianduan: {
    type: Boolean,
    default: true
  },
  showZhongshu: {
    type: Boolean,
    default: true
  },
  showTradingPoints: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['chart-click']);
const { t } = useI18n();

const chartRef = ref(null);

// 处理K线数据格式
const klineValues = computed(() => {
  return props.chartData.klineData.map(item => [
    parseFloat(item.open),
    parseFloat(item.close),
    parseFloat(item.low),
    parseFloat(item.high)
  ]);
});

const dates = computed(() => {
  return props.chartData.klineData.map(item => item.date);
});

const volumes = computed(() => {
  return props.chartData.klineData.map(item => parseFloat(item.volume));
});

// 生成笔的线条数据
const biLines = computed(() => {
  if (!props.showBi) return [];

  return props.chartData.chanTheory.bi.map(bi => ({
    xAxis: bi.startIndex,
    yAxis: parseFloat(bi.startPrice),
    x2Axis: bi.endIndex,
    y2Axis: parseFloat(bi.endPrice),
    lineStyle: {
      color: bi.direction === 'up' ? '#22c55e' : '#ef4444',
      width: 2,
      type: 'solid'
    }
  }));
});

// 生成线段的线条数据
const xianduanLines = computed(() => {
  if (!props.showXianduan) return [];

  return props.chartData.chanTheory.xianduan.map(xd => ({
    xAxis: xd.startIndex,
    yAxis: parseFloat(xd.startPrice),
    x2Axis: xd.endIndex,
    y2Axis: parseFloat(xd.endPrice),
    lineStyle: {
      color: xd.direction === 'up' ? '#10b981' : '#f43f5e',
      width: 3,
      type: 'solid'
    }
  }));
});

// 生成分型标记点
const fenxingMarks = computed(() => {
  if (!props.showFenxing) return [];

  return props.chartData.chanTheory.fenxing.map(fx => ({
    coord: [fx.index, parseFloat(fx.price)],
    symbol: fx.type === 'top' ? 'triangle' : 'triangle',
    symbolRotate: fx.type === 'top' ? 180 : 0,
    symbolSize: 10,
    itemStyle: {
      color: fx.type === 'top' ? '#ef4444' : '#22c55e'
    },
    label: {
      show: false
    }
  }));
});

// 生成买卖点标记
const tradingPointMarks = computed(() => {
  if (!props.showTradingPoints) return [];

  return props.chartData.chanTheory.tradingPoints.map(point => ({
    coord: [point.index, parseFloat(point.price)],
    symbol: 'circle',
    symbolSize: 16,
    itemStyle: {
      color: point.type === 'buy' ? '#22c55e' : '#ef4444',
      borderColor: '#fff',
      borderWidth: 2
    },
    label: {
      show: true,
      formatter: point.type === 'buy' ? `B${point.level}` : `S${point.level}`,
      color: '#fff',
      fontSize: 10,
      fontWeight: 'bold'
    }
  }));
});

// 生成中枢矩形区域
const zhongshuAreas = computed(() => {
  if (!props.showZhongshu) return [];

  return props.chartData.chanTheory.zhongshu.flatMap(zs => [
    // 上轨线
    {
      xAxis: zs.startIndex,
      yAxis: parseFloat(zs.high),
      x2Axis: zs.endIndex,
      y2Axis: parseFloat(zs.high),
      lineStyle: {
        color: '#3b82f6',
        width: 1,
        type: 'dashed'
      }
    },
    // 下轨线
    {
      xAxis: zs.startIndex,
      yAxis: parseFloat(zs.low),
      x2Axis: zs.endIndex,
      y2Axis: parseFloat(zs.low),
      lineStyle: {
        color: '#3b82f6',
        width: 1,
        type: 'dashed'
      }
    },
    // 中轨线
    {
      xAxis: zs.startIndex,
      yAxis: parseFloat(zs.center),
      x2Axis: zs.endIndex,
      y2Axis: parseFloat(zs.center),
      lineStyle: {
        color: '#3b82f6',
        width: 1,
        type: 'dotted'
      }
    }
  ]);
});

// ECharts配置
const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  animation: false,
  grid: [
    {
      left: '3%',
      right: '3%',
      top: '5%',
      height: '60%'
    },
    {
      left: '3%',
      right: '3%',
      top: '70%',
      height: '20%'
    }
  ],
  xAxis: [
    {
      type: 'category',
      data: dates.value,
      boundaryGap: true,
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.8)',
        fontSize: 11
      },
      splitLine: { show: false },
      gridIndex: 0
    },
    {
      type: 'category',
      data: dates.value,
      boundaryGap: true,
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.8)',
        fontSize: 11
      },
      splitLine: { show: false },
      gridIndex: 1
    }
  ],
  yAxis: [
    {
      scale: true,
      splitLine: {
        lineStyle: { color: 'rgba(148, 163, 184, 0.1)' }
      },
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.8)',
        fontSize: 11
      },
      gridIndex: 0
    },
    {
      scale: true,
      splitLine: { show: false },
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)' } },
      axisLabel: {
        color: 'rgba(148, 163, 184, 0.8)',
        fontSize: 11
      },
      gridIndex: 1
    }
  ],
  dataZoom: [
    {
      type: 'inside',
      xAxisIndex: [0, 1],
      start: 50,
      end: 100
    },
    {
      show: true,
      xAxisIndex: [0, 1],
      type: 'slider',
      bottom: '2%',
      start: 50,
      end: 100,
      height: 20,
      borderColor: 'rgba(148, 163, 184, 0.3)',
      fillerColor: 'rgba(59, 130, 246, 0.2)',
      handleStyle: {
        color: 'rgba(59, 130, 246, 0.8)'
      },
      textStyle: {
        color: 'rgba(148, 163, 184, 0.8)'
      }
    }
  ],
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      lineStyle: {
        color: 'rgba(148, 163, 184, 0.5)'
      }
    },
    backgroundColor: 'rgba(15, 23, 42, 0.95)',
    borderColor: 'rgba(148, 163, 184, 0.3)',
    textStyle: {
      color: '#e2e8f0'
    },
    formatter: (params) => {
      const klineParam = params.find(p => p.seriesId === 'kline' || p.seriesType === 'candlestick');
      if (!klineParam) return '';

      const data = klineParam.data;
      const volumeParam = params.find(p => p.seriesId === 'volume' || p.seriesType === 'bar');

      return `
        <div style="padding: 4px;">
          <div style="margin-bottom: 4px; font-weight: bold;">${dates.value[klineParam.dataIndex]}</div>
          <div>${t('chart.open')}: ${data[0]}</div>
          <div>${t('chart.close')}: ${data[1]}</div>
          <div>${t('chart.low')}: ${data[2]}</div>
          <div>${t('chart.high')}: ${data[3]}</div>
          ${volumeParam ? `<div>${t('chart.volume')}: ${volumeParam.data}</div>` : ''}
        </div>
      `;
    }
  },
  series: [
    {
      id: 'kline',
      name: t('chart.kline'),
      type: 'candlestick',
      data: klineValues.value,
      itemStyle: {
        color: '#22c55e',
        color0: '#ef4444',
        borderColor: '#22c55e',
        borderColor0: '#ef4444'
      },
      markPoint: {
        data: [...fenxingMarks.value, ...tradingPointMarks.value]
      },
      markLine: {
        symbol: 'none',
        data: [...biLines.value, ...xianduanLines.value, ...zhongshuAreas.value]
      },
      xAxisIndex: 0,
      yAxisIndex: 0
    },
    {
      id: 'volume',
      name: t('chart.volume'),
      type: 'bar',
      data: volumes.value,
      itemStyle: {
        color: (params) => {
          const kline = klineValues.value[params.dataIndex];
          return kline[1] >= kline[0] ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)';
        }
      },
      xAxisIndex: 1,
      yAxisIndex: 1
    }
  ]
}));

const handleChartClick = (params) => {
  emit('chart-click', params);
};
</script>

<style scoped>
.chan-chart-container {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
