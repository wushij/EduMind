<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Overview</h2>
    </div>
    <div class="page-content">
      <el-card shadow="never">
        <p>Overview 页面正在建设中...</p>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDashboard } from '@/composables/dashboard/useDashboard';

useDashboard();
</script>

<style scoped lang="scss">
.page-container {
  .page-header {
    margin-bottom: 16px;
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #1F2937;
    }
  }
}
</style>
