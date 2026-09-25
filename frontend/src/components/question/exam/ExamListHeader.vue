<template>
  <ModulePageHeroHeader
    :icon="Tickets"
    title="试卷与考试管理中心"
    :badge="`已收录 ${pageReady ? total : '--'} 套标准化试卷`"
    description="集中管理期中/期末统一测试试卷、随堂测验与单元测试，支持双向细目表校验、AI 一键调优换题与格式化导出。"
  >
    <template #stats>
      <ExamListStatsBar
        :page-ready="pageReady"
        :total="total"
        :exam-count="examCount"
      />
    </template>
    <template #actions>
      <button
        type="button"
        class="module-capsule-btn module-capsule-btn--ai"
        @click="routerInstance.push('/ai/exam/generate')"
      >
        <el-icon><AiSparkleIcon /></el-icon>
        <span>AI 智能组卷</span>
        <span class="pill-bubble">双向细目表</span>
      </button>

      <button
        type="button"
        class="module-capsule-btn module-capsule-btn--secondary"
        @click="routerInstance.push('/ai/question/generate')"
      >
        <el-icon><EditPen /></el-icon>
        <span>AI 智能出题</span>
      </button>
    </template>
  </ModulePageHeroHeader>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Tickets, EditPen } from '@element-plus/icons-vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import ExamListStatsBar from '@/components/question/exam/ExamListStatsBar.vue';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';

defineProps<{
  pageReady: boolean;
  total: number;
  examCount: number;
}>();

const routerInstance = useRouter();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
