<template>
  <section class="module-page-stats">
    <div class="module-page-stats__item">
      <el-icon class="text-indigo-600"><Calendar /></el-icon>
      <span>考核学期：</span>
      <strong>{{ examData?.semester || '—' }}</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-blue-600"><Timer /></el-icon>
      <span>考试时限：</span>
      <strong>{{ examData?.durationMinutes ?? 120 }} 分钟</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-emerald-600"><Tickets /></el-icon>
      <span>试卷总分：</span>
      <strong>{{ examData?.totalScore ?? 100 }} 分</strong>
    </div>
    <div class="module-page-stats__item">
      <el-icon class="text-amber-600"><CircleCheck /></el-icon>
      <span>及格线：</span>
      <strong>{{ examData?.passScore ?? 60 }} 分</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Calendar, Timer, Tickets, CircleCheck } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';

defineProps<{
  examData: ExamPaper | null;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
