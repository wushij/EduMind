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
      :difficulty-label="difficultyLabel"
      @back="router.push('/ai/marketplace')"
      @go-to-step="goToStep"
      @toggle-chapter="toggleChapterSelect"
      @toggle-kp="toggleKpSelect"
      @toggle-type="toggleTypeSelect"
      @prev="prevStep"
      @next="nextStep"
      @generate="handleGenerateSubmit"
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
import { useQuestionGenerate } from '@/composables/ai/useQuestionGenerate';
import QuestionGenerateWizard from '@/components/ai/generation/QuestionGenerateWizard.vue';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';
import { MOCK_CHAPTERS } from '@/mock/chapters';
import { QuestionType, Difficulty } from '@/mock/questions';
import questionBannerImg from '@/assets/images/ai智能出题.png';

const router = useRouter();
const { currentStep, generating, courses, formState, nextStep, prevStep, loadCourseOptions, generate } = useQuestionGenerate();

onMounted(() => {
  loadCourseOptions();
});

const displayCourses = computed(() => {
  return courses.value.length ? courses.value : (USE_MOCK ? MOCK_COURSES : []);
});

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

const availableKnowledgePoints = [
  '极限性质与保号性',
  '等价无穷小代换',
  '左右导数与连续性',
  '复合函数链式法则',
  '拉格朗日中值定理',
  '泰勒公式近似展开',
  '反常积分收敛判别',
  '循环队列队满判断'
];

const selectedCourseName = computed(() => {
  const found = displayCourses.value.find(c => c.id === formState.courseId);
  return found?.title || '未指定课程';
});

const currentCourseChapters = computed(() => {
  return MOCK_CHAPTERS[formState.courseId] || MOCK_CHAPTERS[101];
});

const difficultyLabel = computed(() => {
  const found = difficultyOptions.find(d => d.val === formState.difficulty);
  return found?.label || '中等难度';
});

function goToStep(idx: number) {
  if (idx < currentStep.value) {
    currentStep.value = idx;
  }
}

function toggleChapterSelect(id: number) {
  const idx = formState.chapterIds.indexOf(id);
  if (idx > -1) {
    formState.chapterIds.splice(idx, 1);
  } else {
    formState.chapterIds.push(id);
  }
}

function toggleKpSelect(kp: string) {
  const idx = formState.knowledgePointNames.indexOf(kp);
  if (idx > -1) {
    formState.knowledgePointNames.splice(idx, 1);
  } else {
    formState.knowledgePointNames.push(kp);
  }
}

function toggleTypeSelect(type: QuestionType) {
  const idx = formState.questionTypes.indexOf(type);
  if (idx > -1) {
    if (formState.questionTypes.length > 1) {
      formState.questionTypes.splice(idx, 1);
    }
  } else {
    formState.questionTypes.push(type);
  }
}

async function handleGenerateSubmit() {
  await generate();
}
</script>

<style scoped lang="scss">
.question-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;
}
</style>
