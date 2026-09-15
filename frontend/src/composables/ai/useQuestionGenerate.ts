import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Question, QuestionType, Difficulty } from '@/types/question/question';
import { generateQuestions } from '@/api/ai/generation';
import { loadGenerationCourseOptions } from '@/services/ai/generation-service';
import { batchSaveQuestions } from '@/api/question/question';
import { USE_MOCK } from '@/config/mock';
import { MOCK_QUESTIONS } from '@/mock/questions';
import { normalizeQuestionList } from '@/utils/question/normalize-question';

const generatedQuestions = ref<Question[]>([]);

export function useQuestionGenerate() {
  const router = useRouter();
  const currentStep = ref(1);
  const generating = ref(false);
  const courses = ref<any[]>([]);

  const formState = reactive({
    courseId: 101,
    chapterIds: [] as number[],
    knowledgePointIds: [] as number[],
    knowledgePointNames: [] as string[],
    questionTypes: ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'] as QuestionType[],
    difficulty: 'MEDIUM' as Difficulty,
    count: 5,
    scorePerQuestion: 5
  });

  function nextStep() {
    if (currentStep.value < 5) currentStep.value++;
  }

  function prevStep() {
    if (currentStep.value > 1) currentStep.value--;
  }

  async function loadCourseOptions() {
    const list = await loadGenerationCourseOptions();
    courses.value = list;
    if (list.length && !courses.value.some((c) => c.id === formState.courseId)) {
      formState.courseId = courses.value[0].id;
    }
    return courses.value;
  }

  async function generate() {
    generating.value = true;
    try {
      const res = await generateQuestions({
        courseId: formState.courseId,
        chapterIds: formState.chapterIds,
        knowledgePointIds: formState.knowledgePointIds,
        questionTypes: formState.questionTypes,
        difficulty: formState.difficulty,
        count: formState.count,
        scorePerQuestion: formState.scorePerQuestion
      });
      generatedQuestions.value = normalizeQuestionList(res.data || []);
      if (generatedQuestions.value.length === 0 && USE_MOCK) {
        generatedQuestions.value = normalizeQuestionList(MOCK_QUESTIONS);
      }
      ElMessage.success(`AI 出题完成，共生成 ${generatedQuestions.value.length} 道题目`);
      router.push('/ai/question/preview');
    } catch {
      if (USE_MOCK) {
        generatedQuestions.value = normalizeQuestionList(MOCK_QUESTIONS);
        router.push('/ai/question/preview');
      } else {
        ElMessage.error('AI 出题失败，请稍后重试');
      }
    } finally {
      generating.value = false;
    }
  }

  function deleteQuestion(id: number) {
    generatedQuestions.value = generatedQuestions.value.filter(q => q.id !== id);
    ElMessage.info('已剔除该题目');
  }

  async function batchSave() {
    try {
      const res = await batchSaveQuestions(formState.courseId, generatedQuestions.value);
      ElMessage.success(`成功保存 ${res.data?.savedCount ?? generatedQuestions.value.length} 道题目`);
      router.push('/question/list');
    } catch {
      if (USE_MOCK) {
        ElMessage.success(`成功批量保存 ${generatedQuestions.value.length} 道题目`);
        router.push('/question/list');
      } else {
        ElMessage.error('保存失败，请重试');
      }
    }
  }

  return {
    currentStep,
    generating,
    courses,
    formState,
    generatedQuestions,
    nextStep,
    prevStep,
    loadCourseOptions,
    generate,
    deleteQuestion,
    batchSave
  };
}
