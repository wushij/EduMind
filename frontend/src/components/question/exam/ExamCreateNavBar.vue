<template>
  <div class="module-page-hero exam-create-hero">
    <div class="module-page-hero__nav">
      <div class="module-page-hero__nav-inner">
        <button type="button" class="module-page-back" @click="$emit('cancel')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回试卷列表</span>
        </button>
      </div>
    </div>

    <div class="module-page-hero__content">
      <div class="exam-create-hero__head">
        <div class="exam-create-hero__intro">
          <h1 class="exam-create-hero__title">创建试卷</h1>
          <p class="exam-create-hero__desc">
            三步完成基本信息、大题编排与卷面审阅，保存后可发布为在线作业或导出打印。
          </p>
        </div>

        <ol class="exam-create-hero__steps" aria-label="组卷步骤">
          <li
            v-for="(step, index) in steps"
            :key="step.key"
            class="exam-create-hero__step"
            :class="stepClass(index)"
          >
            <span class="exam-create-hero__step-num">{{ index + 1 }}</span>
            <div class="exam-create-hero__step-text">
              <strong>{{ step.title }}</strong>
              <span>{{ step.subtitle }}</span>
            </div>
          </li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';

const props = defineProps<{
  currentStep: number;
}>();

defineEmits<{
  cancel: [];
}>();

const steps = [
  { key: 'basic', title: '试卷基本信息', subtitle: '名称、课程、时限与及格线' },
  { key: 'sections', title: '编排大题与选题', subtitle: '分大题、挑题与配分' },
  { key: 'review', title: '卷面审阅与发布', subtitle: '预览卷面并保存发布' }
] as const;

function stepClass(index: number) {
  if (index === props.currentStep) return 'exam-create-hero__step--active';
  if (index < props.currentStep) return 'exam-create-hero__step--done';
  return '';
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
</style>
