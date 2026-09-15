<template>
  <div class="top-nav-bar">
    <el-button :icon="ArrowLeft" link class="back-link" @click="$emit('cancel')">
      返回试卷列表
    </el-button>
    <div class="step-progress-wrapper">
      <el-steps :active="currentStep" finish-status="success" simple>
        <el-step title="1. 试卷基本信息" :icon="Document" />
        <el-step title="2. 编排大题与选题" :icon="Setting" />
        <el-step title="3. 卷面审阅与发布" :icon="Finished" />
      </el-steps>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Document, Setting, Finished } from '@element-plus/icons-vue';

defineProps<{
  currentStep: number;
}>();

defineEmits<{
  cancel: [];
}>();
</script>

<style scoped lang="scss">
.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;

  .back-link {
    font-size: 14px;
    font-weight: 500;
    color: #3b82f6;
  }

  .step-progress-wrapper {
    flex: 1;
    max-width: 680px;

    :deep(.el-steps--simple) {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 10px 16px;
    }
  }
}
</style>
