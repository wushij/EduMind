<template>
  <section class="module-page-stats">
    <div class="module-page-stats__item">
      <el-icon class="text-blue-600"><Document /></el-icon>
      <span>答卷总数：</span>
      <strong>{{ total }} 份</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-amber-600"><Timer /></el-icon>
      <span>待教师终审：</span>
      <strong>{{ pendingCount }} 份</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-indigo-600"><Cpu /></el-icon>
      <span>AI 已预批：</span>
      <strong>{{ aiGradedCount }} 份</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-emerald-600"><CircleCheck /></el-icon>
      <span>批改完成：</span>
      <strong>{{ gradedCount }} 份</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Document, Timer, Cpu, CircleCheck } from '@element-plus/icons-vue';

defineProps<{
  total: number;
  pendingCount: number;
  aiGradedCount: number;
  gradedCount: number;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
