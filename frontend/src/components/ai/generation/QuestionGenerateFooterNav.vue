<template>
  <div class="wizard-footer-nav">
    <button
      type="button"
      class="capsule-nav-btn capsule-nav-btn--prev"
      :disabled="currentStep === 1"
      @click="$emit('prev')"
    >
      <el-icon><ArrowLeft /></el-icon>
      <span>上一步</span>
    </button>

    <button
      v-if="currentStep < 5"
      type="button"
      class="capsule-nav-btn capsule-nav-btn--next"
      @click="$emit('next')"
    >
      <span>下一步</span>
      <el-icon><ArrowRight /></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';

defineProps<{
  currentStep: number;
}>();

defineEmits<{
  prev: [];
  next: [];
}>();
</script>

<style scoped lang="scss">
.wizard-footer-nav {
  margin-top: 36px;
  padding-top: 24px;
  border-top: 1px solid #F1F5F9;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .capsule-nav-btn {
    height: 42px;
    padding: 0 24px;
    border-radius: 9999px; // 长圆跑道
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &--prev {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      color: #64748B;

      &:hover:not(:disabled) {
        color: #1E293B;
        border-color: #CBD5E1;
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }

    &--next {
      background: #1677FF;
      border: none;
      color: #FFFFFF;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

      &:hover {
        background: #4096FF;
        transform: translateY(-1px);
      }
    }
  }
}
</style>
