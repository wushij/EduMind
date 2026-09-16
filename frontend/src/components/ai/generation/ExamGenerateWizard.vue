<template>
  <div class="ai-teaching-page-shell">
    <ExamGenerateBanner @back="$emit('back')" />

    <div class="mode-switch-bar ai-teaching-surface-card mode-switch-surface">
      <el-radio-group v-model="composeMode">
        <el-radio-button label="quick">快速智能组卷</el-radio-button>
        <el-radio-button label="full">完整试卷编排</el-radio-button>
      </el-radio-group>
    </div>

    <ExamQuickComposePanel
      v-if="composeMode === 'quick'"
      :exam-form="examForm"
      :display-courses="displayCourses"
      v-model:quick-count="quickCount"
      v-model:quick-total-score="quickTotalScore"
      v-model:difficulty-model="difficultyModel"
      :composing="composing"
      :compose-preview="composePreview"
      @quick-compose="$emit('quick-compose')"
      @proceed-preview="$emit('proceed-preview')"
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
      />

      <ExamGenerateFooter
        :exam-form="examForm"
        :calculated-total-score="calculatedTotalScore"
        :is-score-matched="isScoreMatched"
        :generating="generating"
        @generate="$emit('generate')"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import ExamGenerateBanner from './ExamGenerateBanner.vue';
import ExamQuickComposePanel from './ExamQuickComposePanel.vue';
import ExamFullComposeBasicInfo from './ExamFullComposeBasicInfo.vue';
import ExamFullComposeCognitiveLevels from './ExamFullComposeCognitiveLevels.vue';
import ExamFullComposeQuestionRules from './ExamFullComposeQuestionRules.vue';
import ExamGenerateFooter from './ExamGenerateFooter.vue';
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
}>();

defineEmits<{
  back: [];
  'quick-compose': [];
  'proceed-preview': [];
  generate: [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/ai-teaching-page-shell.scss';

.mode-switch-surface {
  padding: 16px 24px;
  margin-bottom: 0;
}
</style>
