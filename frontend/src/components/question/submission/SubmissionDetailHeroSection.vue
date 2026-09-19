<template>
  <div v-loading="loading" class="submission-detail-hero-wrap">
    <ModulePageHeroHeader
      :icon="EditPen"
      :title="String(submissionData?.studentName || '学生答卷')"
      :badge="statusLabel"
      :description="assignmentLine"
    >
      <template #nav>
        <div class="module-page-hero__nav-inner">
          <button type="button" class="module-page-back" @click="router.push('/question/submissions')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回答卷列表</span>
          </button>
          <el-breadcrumb separator="/" class="module-page-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/question/submissions' }">答卷评阅</el-breadcrumb-item>
            <el-breadcrumb-item>{{ submissionData?.studentName || '评阅工作台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </template>

      <template #metaExtra>
        <span class="submission-detail-hero__meta">
          学号 {{ submissionData?.studentNo || '—' }}
          <span class="submission-detail-hero__dot">·</span>
          提交于 {{ formatSubmitTime(submissionData?.submitTime) }}
        </span>
      </template>

      <template #stats>
        <section class="module-page-stats">
          <div class="module-page-stats__item">
            <el-icon class="text-blue-600"><Tickets /></el-icon>
            <span>当前累计得分：</span>
            <strong>{{ totalScore }} / {{ submissionData?.totalScore ?? 100 }} 分</strong>
          </div>
          <div class="module-page-stats__item">
            <el-icon class="text-emerald-600"><List /></el-icon>
            <span>评阅题目：</span>
            <strong>{{ questionCount }} 道</strong>
          </div>
        </section>
      </template>
    </ModulePageHeroHeader>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ArrowLeft,
  EditPen,
  Tickets,
  List
} from '@element-plus/icons-vue';
import type { Router } from 'vue-router';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';

const props = defineProps<{
  router: Router;
  loading: boolean;
  submissionData: Record<string, unknown> | null;
  totalScore: number;
  questionCount: number;
}>();

const statusLabel = computed(() => {
  const s = props.submissionData?.status as string | undefined;
  if (s === 'GRADED') return '已完成教师终审';
  if (s === 'AI_GRADED') return 'AI 已预批 · 待确认';
  return '待教师确认打分';
});

const assignmentLine = computed(() => {
  const title = props.submissionData?.assignmentTitle as string | undefined;
  return title ? `作业任务：${title}` : '学生在线作业答卷评阅工作台';
});

function formatSubmitTime(val: unknown): string {
  if (!val) return '—';
  return String(val).replace('T', ' ').slice(0, 16);
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.submission-detail-hero-wrap {
  :deep(.module-page-stats) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    width: 100%;

    @media (max-width: 720px) {
      grid-template-columns: 1fr;
    }

    .module-page-stats__item {
      justify-content: center;
      width: 100%;
      box-sizing: border-box;
    }
  }
}

.submission-detail-hero__meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.submission-detail-hero__dot {
  color: #cbd5e1;
}
</style>
