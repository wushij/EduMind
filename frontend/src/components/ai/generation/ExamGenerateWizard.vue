<template>
  <div class="ai-teaching-page-shell">
    <ExamGenerateBanner @back="$emit('back')" />

    <div class="mode-switch-bar mode-switch-surface">
      <div class="mode-capsule-wrapper">
        <button
          type="button"
          class="mode-btn"
          :class="{ active: composeMode === 'quick' }"
          @click="composeMode = 'quick'"
        >
          <el-icon><Lightning /></el-icon>
          <span>快速智能组卷 (题库优选与 AI 补齐)</span>
        </button>
        <button
          type="button"
          class="mode-btn"
          :class="{ active: composeMode === 'full' }"
          @click="composeMode = 'full'"
        >
          <el-icon><Operation /></el-icon>
          <span>完整试卷编排 (双向细目表与认知层级)</span>
        </button>
      </div>
    </div>

    <ExamQuickComposePanel
      v-if="composeMode === 'quick'"
      :exam-form="examForm"
      :display-courses="displayCourses"
      v-model:quick-count="quickCount"
      v-model:quick-total-score="quickTotalScore"
      v-model:difficulty-model="difficultyModel"
      :composing="composing"
      :bank-type-stats="bankTypeStats"
      @quick-compose="$emit('quick-compose')"
      @course-change="$emit('course-change', $event)"
    />

    <template v-else>
      <ExamFullComposeBasicInfo
        :exam-form="examForm"
        :display-courses="displayCourses"
      />

      <ExamFullComposeCognitiveLevels
        :cognitive-level-options="cognitiveLevelOptions"
        :cognitive-levels="cognitiveLevels"
        :cognitive-level-total="cognitiveLevelTotal"
      />

      <ExamFullComposeQuestionRules
        :exam-form="examForm"
        :calculated-total-score="calculatedTotalScore"
        :is-score-matched="isScoreMatched"
        :bank-type-stats="bankTypeStats"
        @auto-balance="$emit('auto-balance')"
        @add-rule="(t, l) => $emit('add-rule', t, l)"
        @remove-rule="(t) => $emit('remove-rule', t)"
      />

      <ExamGenerateFooter
        :exam-form="examForm"
        :calculated-total-score="calculatedTotalScore"
        :is-score-matched="isScoreMatched"
        :generating="generating"
        @generate="$emit('generate')"
      />
    </template>

    <!-- AI 智能组卷推演引擎交互弹窗（雷达脉冲动效、秒数计时、流水线推演步骤与中止控制） -->
    <ExamGenerateEngineDialog
      :visible="composing || generating"
      @abort="$emit('abort')"
    />
  </div>
</template>

<script setup lang="ts">
import { Lightning, Operation } from '@element-plus/icons-vue';
import ExamGenerateBanner from './ExamGenerateBanner.vue';
import ExamQuickComposePanel from './ExamQuickComposePanel.vue';
import ExamFullComposeBasicInfo from './ExamFullComposeBasicInfo.vue';
import ExamFullComposeCognitiveLevels from './ExamFullComposeCognitiveLevels.vue';
import ExamFullComposeQuestionRules from './ExamFullComposeQuestionRules.vue';
import ExamGenerateFooter from './ExamGenerateFooter.vue';
import ExamGenerateEngineDialog from './ExamGenerateEngineDialog.vue';
import type { SmartPaperComposeVO } from '@/types/ai/paper-compose';
import type { ExamFormState } from './exam-generate-types';

const composeMode = defineModel<'quick' | 'full'>('composeMode', { required: true });
const quickCount = defineModel<number>('quickCount', { required: true });
const quickTotalScore = defineModel<number>('quickTotalScore', { required: true });
const difficultyModel = defineModel<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('difficultyModel', { required: true });

defineProps<{
  examForm: ExamFormState;
  displayCourses: any[];
  composing: boolean;
  composePreview: SmartPaperComposeVO | null;
  cognitiveLevelOptions: { key: string; label: string }[];
  cognitiveLevels: Record<string, number>;
  cognitiveLevelTotal: number;
  calculatedTotalScore: number;
  isScoreMatched: boolean;
  generating: boolean;
  bankTypeStats?: Record<string, number>;
}>();

defineEmits<{
  (e: 'back'): void;
  (e: 'quick-compose'): void;
  (e: 'proceed-preview'): void;
  (e: 'generate'): void;
  (e: 'abort'): void;
  (e: 'course-change', courseId: number): void;
  (e: 'auto-balance'): void;
  (e: 'add-rule', type: string, label: string): void;
  (e: 'remove-rule', type: string): void;
}>();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.mode-switch-surface {
  background: #ffffff;
  border-radius: 9999px; // 长圆边框
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mode-capsule-wrapper {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 9999px; // 长圆胶囊
  border: 1px solid #e2e8f0;
  gap: 6px;
  width: 100%;

  .mode-btn {
    flex: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 40px;
    border-radius: 9999px; // 长圆胶囊
    border: none;
    background: transparent;
    font-size: 13.5px;
    font-weight: 600;
    color: #64748b;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover:not(.active) {
      color: #1e293b;
    }

    &.active {
      background: #ffffff;
      color: #2563eb;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
    }
  }
}
</style>
