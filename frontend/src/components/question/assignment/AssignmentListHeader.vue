<template>
  <ModulePageHeroHeader
    :icon="Notebook"
    title="课程作业与平时测验管理"
    :badge="`共 ${pageReady ? total : '--'} 份作业任务`"
    description="支持基于试卷或题库布置在线作业，提供客观题自动评阅、主观题 AI 辅助批改与学情统计跟踪。"
  >
    <template #actions>
      <button
        v-permission="'assignment:create'"
        type="button"
        class="module-capsule-btn module-capsule-btn--primary"
        @click="router.push('/question/assignments/create')"
      >
        <el-icon><Plus /></el-icon>
        <span>发布新作业</span>
      </button>
    </template>
  </ModulePageHeroHeader>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Notebook, Plus } from '@element-plus/icons-vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';

defineProps<{
  pageReady: boolean;
  total: number;
}>();

const router = useRouter();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
