<template>
  <div class="lesson-plan-page ai-teaching-page-shell">
    <ProfilePageHero
      title="AI 教案生成"
      subtitle="根据课程主题、学时与教学目标，快速生成结构化教案草案。"
    />

    <div class="ai-teaching-surface-card">
      <el-form :model="form" label-width="100px">
        <el-form-item label="课程 ID">
          <el-input-number v-model="form.courseId" :min="1" />
        </el-form-item>
        <el-form-item label="授课主题">
          <el-input v-model="form.topic" placeholder="例如：二叉树遍历" />
        </el-form-item>
        <el-form-item label="学时">
          <el-input-number v-model="form.hours" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="教学目标">
          <el-input v-model="form.objectives" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleGenerate">生成教案</el-button>
          <el-button @click="copyResult" :disabled="!result">复制结果</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="result" class="ai-teaching-surface-card result-card">
      <div class="markdown-body" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import { useLessonPlan } from '@/composables/ai/useLessonPlan';

const { form, loading, result, renderedHtml, handleGenerate, copyResult } = useLessonPlan();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.result-card {
  margin-top: 0;
}

.markdown-body {
  line-height: 1.7;
  color: #334155;
}
</style>
