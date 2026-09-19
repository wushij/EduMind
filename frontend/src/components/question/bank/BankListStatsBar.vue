<template>
  <section class="module-page-stats">
    <div class="module-page-stats__item">
      <el-icon class="text-blue-600"><FolderOpened /></el-icon>
      <span>精选题库：</span>
      <strong>{{ total }} 个</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-emerald-600"><DocumentCopy /></el-icon>
      <span>收录试题：</span>
      <strong>{{ totalQuestions }} 道</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-indigo-600"><Reading /></el-icon>
      <span>关联课程：</span>
      <strong>{{ courseCount }} 门</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { DocumentCopy, FolderOpened, Reading } from '@element-plus/icons-vue';

defineProps<{
  total: number;
  totalQuestions: number;
  courseCount: number;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
