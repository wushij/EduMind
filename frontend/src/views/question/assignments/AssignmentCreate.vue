<template>
  <div class="assignment-create-container">
    <!-- 顶部返回与标题 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="handleCancel">
        返回作业列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>发布新作业</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <AssignmentCreateForm
      :set-form-ref="setFormRef"
      :form-data="formData"
      :rules="rules"
      :courses="courses"
      :exam-options="examOptions"
      v-model:source-mode="sourceMode"
      :selected-questions="selectedQuestions"
      :total-calculated-score="totalCalculatedScore"
      :submitting="submitting"
      :on-course-change="handleCourseChange"
      :on-exam-selected="handleExamSelected"
      :get-type-label="getTypeLabel"
      :get-type-tag-type="getTypeTagType"
      @cancel="handleCancel"
      @publish="handlePublishAssignment"
    />
  </div>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useAssignmentCreate } from '@/composables/question/useAssignment';
import AssignmentCreateForm from '@/components/question/assignment/AssignmentCreateForm.vue';

const {
  formRef,
  submitting,
  courses,
  examOptions,
  sourceMode,
  formData,
  rules,
  selectedQuestions,
  totalCalculatedScore,
  handleCourseChange,
  handleExamSelected,
  handlePublishAssignment,
  handleCancel,
  getTypeLabel,
  getTypeTagType
} = useAssignmentCreate();

function setFormRef(el: FormInstance | undefined) {
  formRef.value = el;
}
</script>

<style scoped lang="scss">
.assignment-create-container {
  padding: 24px;
  background: #f8fafc;
  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }
}
</style>
