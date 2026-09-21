<template>
  <div class="question-edit-page question-module-page">
    <div class="page-top-bar">
      <div class="left-nav">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回试题列表
        </el-button>
        <el-divider direction="vertical" />
        <h1 class="page-title">编辑试题（#{{ route.params.id }}）</h1>
      </div>
      <span class="page-sub">修改题干、调整选项与标准答案，或更新解题思路与考察知识点</span>
    </div>

    <div v-loading="loading" class="page-body">
      <QuestionForm
        v-if="questionData"
        :model-value="questionData"
        :is-edit="true"
        :submitting="submitting"
        @submit="handleFormSubmit"
        @cancel="router.back()"
      />
      <el-empty v-else-if="!loading" description="未找到对应试题，可能已被删除" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import QuestionForm from '@/components/question/QuestionForm.vue';
import { useQuestion } from '@/composables/question/useQuestion';
import type { Question } from '@/types/question/question';

const router = useRouter();
const route = useRoute();
const { fetchQuestionDetail, saveQuestion } = useQuestion();

// 题目 ID 可能是 AI 生成题的雪花 ID（19 位），Number() 会丢精度导致查不到详情，这里保留字符串
const questionId = String(route.params.id ?? '');
const questionData = ref<Question | null>(null);
const loading = ref(true);
const submitting = ref(false);

onMounted(async () => {
  if (!questionId) {
    ElMessage.error('试题 ID 缺失');
    router.back();
    return;
  }
  try {
    const detail = await fetchQuestionDetail(questionId);
    if (detail) {
      questionData.value = detail;
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载试题失败');
  } finally {
    loading.value = false;
  }
});

async function handleFormSubmit(formData: Partial<Question>) {
  submitting.value = true;
  try {
    await saveQuestion(formData, questionId);
    ElMessage.success('试题修改保存成功！');
    router.push('/question/list');
  } catch (err: any) {
    ElMessage.error(err?.message || '保存修改失败，请重试');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.question-edit-page {
  .page-top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e2e8f0;

    .left-nav {
      display: flex;
      align-items: center;
      gap: 12px;

      .page-title {
        font-size: 20px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
      }
    }

    .page-sub {
      font-size: 13px;
      color: #64748b;
    }
  }

  .page-body {
    width: 100%;
    min-height: 400px;
  }
}
</style>
