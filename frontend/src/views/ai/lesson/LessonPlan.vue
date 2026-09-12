<template>
  <div class="lesson-plan-page">
    <h2>AI 教案生成</h2>
    <el-card shadow="never">
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
    </el-card>
    <el-card v-if="result" class="result-card" shadow="never">
      <div class="markdown-body" v-html="renderedHtml"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { generateLessonPlan } from '@/api/ai/lesson';

const form = ref({
  courseId: 1,
  topic: '',
  hours: 2,
  objectives: ''
});
const loading = ref(false);
const result = ref('');

const renderedHtml = computed(() =>
  result.value
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\n/g, '<br/>')
);

async function handleGenerate() {
  if (!form.value.topic.trim()) {
    ElMessage.warning('请输入授课主题');
    return;
  }
  loading.value = true;
  try {
    const res = await generateLessonPlan(form.value);
    result.value = res?.data?.content || '';
    ElMessage.success('教案生成完成');
  } catch {
    ElMessage.error('教案生成失败');
  } finally {
    loading.value = false;
  }
}

function copyResult() {
  navigator.clipboard.writeText(result.value);
  ElMessage.success('已复制到剪贴板');
}
</script>

<style scoped lang="scss">
.lesson-plan-page {
  padding: 24px;

  h2 {
    margin: 0 0 16px;
  }

  .result-card {
    margin-top: 16px;
  }

  .markdown-body {
    line-height: 1.7;
    color: #334155;
  }
}
</style>
