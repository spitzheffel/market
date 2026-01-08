<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">ChanLun Pro</div>
      <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline">
        <a-menu-item key="1">
          <template #icon>
            <stock-outlined />
          </template>
          <span>Market Analysis</span>
        </a-menu-item>
        <a-menu-item key="2">
          <template #icon>
            <setting-outlined />
          </template>
          <span>Settings</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #1f1f1f; padding: 0 16px; color: white; display: flex; align-items: center; justify-content: space-between;">
        <span>BTC/USDT - Binance</span>
        <div>User</div>
      </a-layout-header>
      <a-layout-content style="margin: 0; background: #141414;">
        <slot></slot>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref } from 'vue';
import { StockOutlined, SettingOutlined } from '@ant-design/icons-vue';

const collapsed = ref(false);
const selectedKeys = ref(['1']);
</script>

<style scoped>
.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.2);
  text-align: center;
  line-height: 32px;
  color: #fff;
  font-weight: bold;
}
</style>
