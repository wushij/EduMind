<template>
  <div class="summary-page ai-teaching-page-shell">
    <ProfilePageHero
      title="AI 课程总结"
      subtitle="基于文档或粘贴文本，自动生成课程要点与结构化摘要。"
    />

    <div class="ai-teaching-surface-card">
      <el-form label-width="100px">
        <el-form-item label="文档 ID">
          <el-input-number v-model="documentId" :min="1" />
        </el-form-item>
        <el-form-item label="或粘贴文本">
          <el-input v-model="content" type="textarea" :rows="6" placeholder="可直接粘贴课件或讲义文本" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSummarize">生成总结</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="result" class="ai-teaching-surface-card result-card">
      <pre class="summary-output">{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import { useLessonSummary } from '@/composables/ai/useLessonSummary';

const { documentId, content, loading, result, handleSummarize } = useLessonSummary();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.summary-output {
  white-space: pre-wrap;
  line-height: 1.7;
  margin: 0;
}
</style>
