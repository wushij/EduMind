<template>
  <section class="module-page-stats">
    <div class="module-page-stats__item">
      <el-icon class="text-blue-600"><Document /></el-icon>
      <span>试卷总数：</span>
      <strong>{{ pageReady ? `${total} 套` : '--' }}</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-emerald-600"><CircleCheck /></el-icon>
      <span>当前页展示：</span>
      <strong>{{ pageReady ? `${examCount} 套` : '--' }}</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Document, CircleCheck } from '@element-plus/icons-vue';

defineProps<{
  pageReady: boolean;
  total: number;
  examCount: number;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
