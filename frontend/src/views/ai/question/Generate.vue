<template>
  <div class="question-generate-page">
    <QuestionGenerateWizard
      :steps="steps"
      :current-step="currentStep"
      :generating="generating"
      :display-courses="displayCourses"
      :form-state="formState"
      :current-course-chapters="courseChapters"
      :available-knowledge-points="courseKnowledgePoints"
      :loading-chapters="loadingChapters"
      :loading-kps="loadingKps"
      :type-options="typeOptions"
      :difficulty-options="difficultyOptions"
      :selected-course-name="selectedCourseName"
      :difficulty-label="difficultyLabel(formState.difficulty)"
      @back="router.push('/ai/marketplace')"
      @go-to-step="goToStep"
      @toggle-chapter="toggleChapterSelect"
      @select-all-chapters="selectAllChapters"
      @clear-chapters="clearChapters"
      @toggle-kp="toggleKpSelect"
      @toggle-type="toggleTypeSelect"
      @prev="prevStep"
      @next="nextStep"
      @generate="generate"
    />

    <!-- AI 智能命题推演引擎交互弹窗（雷达脉冲动效、秒数计时、流水线步骤与中止控制） -->
    <QuestionGenerateEngineDialog
      :visible="generating"
      @abort="abortGeneration"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, markRaw } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  CircleCheck,
  Finished,
  ScaleToOriginal,
  EditPen,
  Document
} from '@element-plus/icons-vue';
import { useQuestionGenerate } from '@/composables/ai/useQuestionGenerate';
import QuestionGenerateWizard from '@/components/ai/generation/QuestionGenerateWizard.vue';
import QuestionGenerateEngineDialog from '@/components/ai/generation/QuestionGenerateEngineDialog.vue';
import type { QuestionType, Difficulty } from '@/types/question/question';

const router = useRouter();
const route = useRoute();
const {
  currentStep,
  generating,
  displayCourses,
  formState,
  courseChapters,
  courseKnowledgePoints,
  loadingChapters,
  loadingKps,
  selectedCourseName,
  nextStep,
  prevStep,
  goToStep,
  toggleChapterSelect,
  selectAllChapters,
  clearChapters,
  toggleKpSelect,
  toggleTypeSelect,
  difficultyLabel,
  loadCourseOptions,
  generate,
  abortGeneration
} = useQuestionGenerate();

onMounted(async () => {
  await loadCourseOptions();
  if (route.query.courseId) {
    formState.courseId = Number(route.query.courseId);
  }
  if (route.query.targetBankId) {
    formState.targetBankId = String(route.query.targetBankId);
    if (route.query.bankName) {
      formState.targetBankName = decodeURIComponent(String(route.query.bankName));
    }
    if (currentStep.value === 1 && formState.courseId) {
      currentStep.value = 2;
    }
  }
});

const steps = [
  { index: 1, name: '选择课程' },
  { index: 2, name: '考察范围' },
  { index: 3, name: '题型策略' },
  { index: 4, name: '题量分值' },
  { index: 5, name: '确认生成' }
];

const typeOptions: { type: QuestionType; label: string; desc: string; icon: any; color: string }[] = [
  { type: 'SINGLE_CHOICE', label: '单选题', desc: '4选1概念/推导题', icon: markRaw(CircleCheck), color: '#1677FF' },
  { type: 'MULTIPLE_CHOICE', label: '多选题', desc: '多项综合辨析', icon: markRaw(Finished), color: '#0284C7' },
  { type: 'TRUE_FALSE', label: '判断题', desc: '定理边界与是非辨析', icon: markRaw(ScaleToOriginal), color: '#10B981' },
  { type: 'FILL_BLANK', label: '填空题', desc: '核心结论/参数填空', icon: markRaw(EditPen), color: '#F59E0B' },
  { type: 'SHORT_ANSWER', label: '简答分析题', desc: '算法设计/综合论述', icon: markRaw(Document), color: '#8B5CF6' }
];

const difficultyOptions: { val: Difficulty; label: string; desc: string; colorClass: string }[] = [
  { val: 'EASY', label: '简单', desc: '考查基础定义与概念识记（识记/理解）', colorClass: 'diff-easy' },
  { val: 'MEDIUM', label: '中等', desc: '考查综合运用与定理推导（应用/分析）', colorClass: 'diff-medium' },
  { val: 'HARD', label: '困难', desc: '考查难题辨析与复杂建模（评价/设计）', colorClass: 'diff-hard' }
];
</script>

<style scoped lang="scss">
.question-generate-page {
  width: 100%;
  box-sizing: border-box;
}
</style>
