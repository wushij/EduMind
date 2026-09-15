<template>
  <div class="exam-generate-page">
    <ExamGenerateWizard
      :banner-img="examBannerImg"
      v-model:compose-mode="composeMode"
      v-model:quick-count="quickCount"
      v-model:quick-total-score="quickTotalScore"
      v-model:difficulty-model="difficultyModel"
      :exam-form="examForm"
      :display-courses="displayCourses"
      :composing="composing"
      :compose-preview="composePreview"
      :cognitive-level-options="cognitiveLevelOptions"
      :cognitive-levels="cognitiveLevels"
      :cognitive-level-total="cognitiveLevelTotal"
      :calculated-total-score="calculatedTotalScore"
      :is-score-matched="isScoreMatched"
      :generating="generating"
      @back="router.push('/ai/marketplace')"
      @quick-compose="handleQuickCompose"
      @proceed-preview="handleProceedToPreview"
      @generate="handleGenerateExam"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useExamGenerate } from '@/composables/ai/useExamGenerate';
import ExamGenerateWizard from '@/components/ai/generation/ExamGenerateWizard.vue';
import type { SmartPaperComposeVO } from '@/types/ai/paper-compose';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';
import { normalizeQuestion } from '@/utils/question/normalize-question';
import { ElMessage } from 'element-plus';
import examBannerImg from '@/assets/images/ai组卷.png';

const router = useRouter();
const composeMode = ref<'quick' | 'full'>('quick');
const quickCount = ref(10);
const quickTotalScore = ref(100);
const difficultyModel = ref<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('NORMAL');
const composePreview = ref<SmartPaperComposeVO | null>(null);

const cognitiveLevelOptions = [
  { key: 'REMEMBER', label: '识记 (Remember)' },
  { key: 'UNDERSTAND', label: '理解 (Understand)' },
  { key: 'APPLY', label: '应用 (Apply)' },
  { key: 'ANALYZE', label: '分析 (Analyze)' },
  { key: 'EVALUATE', label: '评价 (Evaluate)' }
];

const cognitiveLevels = ref<Record<string, number>>({
  REMEMBER: 15,
  UNDERSTAND: 25,
  APPLY: 30,
  ANALYZE: 20,
  EVALUATE: 10
});

const cognitiveLevelTotal = computed(() =>
  Object.values(cognitiveLevels.value).reduce((sum, val) => sum + val, 0)
);

const {
  examForm,
  currentExam,
  generating,
  composing,
  courses,
  calculatedTotalScore,
  isScoreMatched,
  loadCourseOptions,
  composeSmartPaper
} = useExamGenerate();

const getDifficultyDistribution = () => {
  if (difficultyModel.value === 'FOUNDATION') {
    return { EASY: 0.5, MEDIUM: 0.4, HARD: 0.1 };
  }
  if (difficultyModel.value === 'ADVANCED') {
    return { EASY: 0.1, MEDIUM: 0.4, HARD: 0.5 };
  }
  return { EASY: 0.3, MEDIUM: 0.5, HARD: 0.2 };
};

onMounted(() => {
  loadCourseOptions();
});

const displayCourses = computed(() => {
  return courses.value.length ? courses.value : (USE_MOCK ? MOCK_COURSES : []);
});

function buildTypeRatios() {
  const totalCount = examForm.rules.reduce((sum, rule) => sum + rule.count, 0);
  if (totalCount <= 0) return {};
  const ratios: Record<string, number> = {};
  examForm.rules.forEach((rule) => {
    if (rule.count > 0) {
      ratios[rule.type] = rule.count / totalCount;
    }
  });
  return ratios;
}

function buildCognitiveLevelRatios() {
  const total = cognitiveLevelTotal.value || 1;
  const ratios: Record<string, number> = {};
  Object.entries(cognitiveLevels.value).forEach(([key, val]) => {
    if (val > 0) ratios[key] = val / total;
  });
  return ratios;
}

async function handleGenerateExam() {
  if (!isScoreMatched.value) {
    ElMessage.warning('请先调整题型分值使总分匹配');
    return;
  }
  generating.value = true;
  try {
    const totalCount = examForm.rules.reduce((sum, rule) => sum + rule.count, 0);
    const preview = await composeSmartPaper({
      courseId: examForm.courseId,
      totalCount,
      totalScore: examForm.totalScore,
      difficultyDistribution: getDifficultyDistribution(),
      typeRatios: buildTypeRatios(),
      cognitiveLevels: buildCognitiveLevelRatios()
    });
    if (!preview?.questions?.length) {
      ElMessage.warning('未抽到题目，请检查题库');
      return;
    }
    currentExam.value = {
      id: Date.now(),
      courseId: examForm.courseId,
      courseName: displayCourses.value.find((c) => c.id === examForm.courseId)?.title ?? '',
      title: examForm.title,
      semester: '',
      totalScore: preview.totalScore || examForm.totalScore,
      durationMinutes: examForm.durationMinutes,
      passScore: Math.round((preview.totalScore || examForm.totalScore) * 0.6),
      rules: [...examForm.rules],
      questions: preview.questions.map((q: any) =>
        normalizeQuestion({
          ...q,
          courseId: examForm.courseId,
          score: q.score ?? Math.round(examForm.totalScore / totalCount)
        })
      ),
      createdAt: new Date().toISOString().split('T')[0]
    };
    ElMessage.success(`完整模式组卷完成，知识点覆盖率 ${((preview.coverageRate || 0) * 100).toFixed(1)}%`);
    router.push('/ai/exam/preview');
  } catch (err: any) {
    ElMessage.error(err?.message || '完整模式智能组卷失败');
  } finally {
    generating.value = false;
  }
}

async function handleQuickCompose() {
  try {
    composePreview.value = await composeSmartPaper({
      courseId: examForm.courseId,
      totalCount: quickCount.value,
      totalScore: quickTotalScore.value,
      difficultyDistribution: getDifficultyDistribution(),
      typeRatios: {
        SINGLE_CHOICE: 0.5,
        MULTIPLE_CHOICE: 0.3,
        JUDGE: 0.2
      }
    });
    if (!composePreview.value?.questions?.length) {
      ElMessage.warning('未抽到题目，请检查题库');
      return;
    }
    ElMessage.success(`智能组卷 v2 运算完成！知识点覆盖率: ${((composePreview.value.coverageRate || 0) * 100).toFixed(1)}%`);
  } catch (err: any) {
    ElMessage.error(err?.message || '快速智能组卷运算失败');
  }
}

function handleProceedToPreview() {
  if (!composePreview.value?.questions?.length) return;
  currentExam.value = {
    id: Date.now(),
    courseId: examForm.courseId,
    courseName: displayCourses.value.find((c) => c.id === examForm.courseId)?.title ?? '',
    title: examForm.title || `智能组卷 · 难度梯度(${difficultyModel.value === 'FOUNDATION' ? '基础' : difficultyModel.value === 'ADVANCED' ? '拔高' : '标准'})综合测试`,
    semester: '',
    totalScore: composePreview.value.totalScore || quickTotalScore.value,
    durationMinutes: examForm.durationMinutes,
    passScore: Math.round((composePreview.value.totalScore || quickTotalScore.value) * 0.6),
    rules: [],
    questions: composePreview.value.questions.map((q: any) =>
      normalizeQuestion({
        ...q,
        courseId: examForm.courseId,
        score: q.score ?? 5
      })
    ),
    createdAt: new Date().toISOString().split('T')[0]
  };
  ElMessage.success('试卷规划生成就绪，已进入预览发布页');
  router.push('/ai/exam/preview');
}
</script>

<style scoped lang="scss">
.exam-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;
}
</style>
