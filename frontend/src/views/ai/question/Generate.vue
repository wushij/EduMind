<template>
  <div class="question-generate-page">
    <QuestionGenerateWizard
      :banner-img="questionBannerImg"
      :steps="steps"
      :current-step="currentStep"
      :generating="generating"
      :display-courses="displayCourses"
      :form-state="formState"
      :current-course-chapters="currentCourseChapters"
      :available-knowledge-points="availableKnowledgePoints"
      :type-options="typeOptions"
      :difficulty-options="difficultyOptions"
      :selected-course-name="selectedCourseName"
      :difficulty-label="difficultyLabel(formState.difficulty)"
      @back="router.push('/ai/marketplace')"
      @go-to-step="goToStep"
      @toggle-chapter="toggleChapterSelect"
      @toggle-kp="toggleKpSelect"
      @toggle-type="toggleTypeSelect"
      @prev="prevStep"
      @next="nextStep"
      @generate="generate"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import {
  CircleCheck,
  Finished,
  ScaleToOriginal,
  EditPen,
  Document
} from '@element-plus/icons-vue';
import {
  useQuestionGenerate,
  QUESTION_GENERATE_KNOWLEDGE_POINTS
} from '@/composables/ai/useQuestionGenerate';
import QuestionGenerateWizard from '@/components/ai/generation/QuestionGenerateWizard.vue';
import type { QuestionType, Difficulty } from '@/types/question/question';
import questionBannerImg from '@/assets/images/ai智能出题.png';

const router = useRouter();
const {
  currentStep,
  generating,
  displayCourses,
  formState,
  currentCourseChapters,
  selectedCourseName,
  nextStep,
  prevStep,
  goToStep,
  toggleChapterSelect,
  toggleKpSelect,
  toggleTypeSelect,
  difficultyLabel,
  loadCourseOptions,
  generate
} = useQuestionGenerate();

onMounted(() => {
  loadCourseOptions();
});

const availableKnowledgePoints = QUESTION_GENERATE_KNOWLEDGE_POINTS;

const steps = [
  { index: 1, name: '选择课程' },
  { index: 2, name: '考察范围' },
  { index: 3, name: '题型难度' },
  { index: 4, name: '题量分值' },
  { index: 5, name: '确认生成' }
];

const typeOptions: { type: QuestionType; label: string; icon: any; color: string }[] = [
  { type: 'SINGLE_CHOICE', label: '单选题', icon: markRaw(CircleCheck), color: '#1677FF' },
  { type: 'MULTIPLE_CHOICE', label: '多选题', icon: markRaw(Finished), color: '#0284C7' },
  { type: 'TRUE_FALSE', label: '判断题', icon: markRaw(ScaleToOriginal), color: '#10B981' },
  { type: 'FILL_BLANK', label: '填空题', icon: markRaw(EditPen), color: '#F59E0B' },
  { type: 'SHORT_ANSWER', label: '简答分析题', icon: markRaw(Document), color: '#8B5CF6' }
];

const difficultyOptions: { val: Difficulty; label: string; desc: string; colorClass: string }[] = [
  { val: 'EASY', label: '简单', desc: '考查基础定义与概念识记', colorClass: 'diff-easy' },
  { val: 'MEDIUM', label: '中等', desc: '考查综合运用与定理推导', colorClass: 'diff-medium' },
  { val: 'HARD', label: '困难', desc: '考查难题辨析与复杂建模', colorClass: 'diff-hard' }
];
</script>

<style scoped lang="scss">
.question-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;
}
</style>
