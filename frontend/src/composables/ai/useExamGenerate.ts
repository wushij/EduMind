import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Exam, ExamRule } from '@/types/question/exam';
import { composeSmartPaperV2 } from '@/api/ai/paper-compose';
import { loadGenerationCourseOptions } from '@/services/ai/generation-service';
import { generateExam, createExam } from '@/api/question/exam';
import { USE_MOCK } from '@/config/mock';
import { MOCK_EXAMS } from '@/mock/exams';
import type { SmartPaperComposeRequest, SmartPaperComposeVO } from '@/types/ai/paper-compose';

const currentExam = ref<Exam>({ ...MOCK_EXAMS[0] });

export function useExamGenerate() {
  const router = useRouter();
  const generating = ref(false);
  const composing = ref(false);
  const courses = ref<any[]>([]);

  const examForm = reactive({
    courseId: 101,
    title: '期中测验',
    totalScore: 100,
    durationMinutes: 90,
    chapterIds: [] as number[],
    rules: [
      { type: 'SINGLE_CHOICE', label: '单项选择题', count: 10, scoreEach: 3 },
      { type: 'MULTIPLE_CHOICE', label: '多项选择题', count: 5, scoreEach: 4 },
      { type: 'SHORT_ANSWER', label: '解答题', count: 3, scoreEach: 10 }
    ] as ExamRule[]
  });

  const calculatedTotalScore = computed(() =>
    examForm.rules.reduce((acc, cur) => acc + cur.count * cur.scoreEach, 0)
  );

  const isScoreMatched = computed(() => calculatedTotalScore.value === examForm.totalScore);

  async function loadCourseOptions() {
    const list = await loadGenerationCourseOptions();
    courses.value = list;
    if (list.length && !courses.value.some((c) => c.id === examForm.courseId)) {
      examForm.courseId = courses.value[0].id;
    }
    return courses.value;
  }

  async function composeSmartPaper(request: SmartPaperComposeRequest): Promise<SmartPaperComposeVO | null> {
    composing.value = true;
    try {
      const res = await composeSmartPaperV2(request);
      return res.data ?? null;
    } finally {
      composing.value = false;
    }
  }

  async function generateExamPreview() {
    if (!isScoreMatched.value) {
      ElMessage.warning(`各题型总分 ${calculatedTotalScore.value} 分，需等于目标总分 ${examForm.totalScore} 分`);
      return;
    }
    generating.value = true;
    try {
      const res = await generateExam({
        courseId: examForm.courseId,
        title: examForm.title,
        totalScore: examForm.totalScore,
        durationMinutes: examForm.durationMinutes,
        chapterIds: examForm.chapterIds,
        rules: examForm.rules.map(r => ({ type: r.type, count: r.count, scoreEach: r.scoreEach }))
      });
      currentExam.value = {
        ...(res.data as Exam),
        id: res.data?.id || Date.now(),
        courseId: examForm.courseId,
        courseName: res.data?.courseName || '',
        title: examForm.title,
        semester: res.data?.semester || '',
        totalScore: examForm.totalScore,
        durationMinutes: examForm.durationMinutes,
        passScore: Math.round(examForm.totalScore * 0.6),
        rules: [...examForm.rules],
        questions: res.data?.questions || [],
        createdAt: new Date().toISOString().split('T')[0]
      };
      ElMessage.success('智能组卷成功');
      router.push('/ai/exam/preview');
    } catch {
      if (USE_MOCK) {
        currentExam.value = { ...MOCK_EXAMS[0], title: examForm.title };
        router.push('/ai/exam/preview');
      } else {
        ElMessage.error('组卷失败，请重试');
      }
    } finally {
      generating.value = false;
    }
  }

  function swapQuestion(_questionId: number) {
    ElMessage.info('已从题库抽取替补题目');
  }

  async function saveExam() {
    try {
      await createExam({
        courseId: currentExam.value.courseId,
        title: currentExam.value.title,
        totalScore: currentExam.value.totalScore,
        durationMinutes: currentExam.value.durationMinutes,
        questions: currentExam.value.questions
      });
      ElMessage.success(`试卷【${currentExam.value.title}】保存成功`);
      router.push('/question/exams');
    } catch {
      if (USE_MOCK) {
        ElMessage.success(`试卷【${currentExam.value.title}】保存成功`);
        router.push('/question/exams');
      } else {
        ElMessage.error('保存试卷失败');
      }
    }
  }

  return {
    examForm,
    currentExam,
    generating,
    composing,
    courses,
    calculatedTotalScore,
    isScoreMatched,
    loadCourseOptions,
    composeSmartPaper,
    generateExam: generateExamPreview,
    swapQuestion,
    saveExam
  };
}
