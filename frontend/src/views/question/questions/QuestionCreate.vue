<template>
  <div class="question-create-page">
    <!-- 顶部导航条与面包屑 -->
    <div class="page-top-bar">
      <div class="left-nav">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回试题列表
        </el-button>
        <el-divider direction="vertical" />
        <h1 class="page-title">录入新试题</h1>
      </div>
      <span class="page-sub">创建单选、多选、判断、填空或主观推导题，支持公式与采分点设置</span>
    </div>

    <!-- 主表单 -->
    <div class="page-body">
      <QuestionForm
        :submitting="submitting"
        @submit="handleFormSubmit"
        @cancel="router.back()"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import QuestionForm from '@/components/question/QuestionForm.vue';
import { useQuestion } from '@/composables/question/useQuestion';
import type { Question } from '@/types/question/question';

const router = useRouter();
const { saveQuestion } = useQuestion();
const submitting = ref(false);

async function handleFormSubmit(formData: Partial<Question>) {
  submitting.value = true;
  try {
    await saveQuestion(formData);
    ElMessage.success('试题录入入库成功！');
    router.push('/question/list');
  } catch (err: any) {
    ElMessage.error(err?.message || '保存试题失败，请检查必填项');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
.question-create-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;

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
  }
}
</style>
