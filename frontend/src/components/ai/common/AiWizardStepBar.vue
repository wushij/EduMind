<template>
  <div class="ai-wizard-step-bar">
    <div
      v-for="step in steps"
      :key="step.index"
      class="wizard-step-item"
      :class="{
        active: currentStep === step.index,
        completed: currentStep > step.index
      }"
      @click="$emit('go-to-step', step.index)"
    >
      <span class="step-num">
        <el-icon v-if="currentStep > step.index"><Check /></el-icon>
        <span v-else>{{ step.index }}</span>
      </span>
      <span class="step-name">{{ step.name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue';

defineProps<{
  steps: { index: number; name: string }[];
  currentStep: number;
}>();

defineEmits<{
  'go-to-step': [index: number];
}>();
</script>

<style scoped lang="scss">
.ai-wizard-step-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 18px rgba(22, 119, 255, 0.08);

  .wizard-step-item {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 500;
    color: #64748b;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;

    .step-num {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #f1f5f9;
      color: #475569;
      font-size: 11.5px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;
    }

    &:hover:not(.active) {
      color: #1677ff;
      background: rgba(239, 246, 255, 0.75);
    }

    &.active {
      background: #1677ff;
      color: #ffffff;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);

      .step-num {
        background: #ffffff;
        color: #1677ff;
      }
    }

    &.completed {
      color: #1677ff;

      .step-num {
        background: #eff6ff;
        color: #1677ff;
      }
    }
  }
}

@media (max-width: 900px) {
  .ai-wizard-step-bar {
    border-radius: 20px;

    .wizard-step-item {
      padding: 6px 12px;
      font-size: 12px;
    }
  }
}
</style>
