<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#1890ff',
        colorBgBase: '#141414',
        colorTextBase: '#ffffff',
      },
      algorithm: theme.darkAlgorithm,
    }"
  >
    <router-view />
  </a-config-provider>
</template>

<script setup>
import { theme } from 'ant-design-vue';
</script>

<style>
/* Global styles handled in style.css */
</style>
