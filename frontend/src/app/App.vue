<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <MermaidViewerModal />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import MermaidViewerModal from '@/components/common/MermaidViewerModal.vue';
</script>

<style>
/* Global App Root Styles */
</style>
