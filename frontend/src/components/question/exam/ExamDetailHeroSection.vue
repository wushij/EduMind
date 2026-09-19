<template>
  <div v-loading="loading" class="exam-detail-hero-wrap">
    <ModulePageHeroHeader
      :icon="Tickets"
      :title="examData?.title || '考核试卷详情'"
      :badge="getStatusLabel(examData?.status)"
      :description="examData?.courseName ? `所属课程：${examData.courseName}` : undefined"
    >
      <template #nav>
        <div class="module-page-hero__nav-inner">
          <button type="button" class="module-page-back" @click="router.push('/question/exams')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回试卷列表</span>
          </button>
          <el-breadcrumb separator="/" class="module-page-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/question/exams' }">试卷管理</el-breadcrumb-item>
            <el-breadcrumb-item>{{ examData?.title || '试卷详情' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </template>

      <template #stats>
        <ExamDetailStatsBar :exam-data="examData" />
      </template>

      <template #actions>
        <button type="button" class="module-capsule-btn module-capsule-btn--primary" @click="emit('publish')">
          <el-icon><Promotion /></el-icon>
          <span>发布为在线作业</span>
        </button>
        <button type="button" class="module-capsule-btn module-capsule-btn--secondary" @click="emit('export')">
          <el-icon><Download /></el-icon>
          <span>考务排版导出</span>
        </button>
        <button type="button" class="module-capsule-btn module-capsule-btn--danger" @click="emit('delete')">
          <el-icon><Delete /></el-icon>
          <span>删除试卷</span>
        </button>
      </template>
    </ModulePageHeroHeader>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Promotion, Download, Tickets, Delete } from '@element-plus/icons-vue';
import type { ExamPaper } from '@/types/question/exam';
import type { Router } from 'vue-router';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import ExamDetailStatsBar from '@/components/question/exam/ExamDetailStatsBar.vue';

defineProps<{
  router: Router;
  loading: boolean;
  examData: ExamPaper | null;
  getStatusLabel: (status?: string) => string;
  getStatusTagType: (status?: string) => string;
}>();

const emit = defineEmits<{
  publish: [];
  export: [];
  delete: [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
