import { ref } from 'vue';
import {
  getQuestions,
  getQuestionDetail,
  createQuestion,
  updateQuestion,
  deleteQuestion
} from '@/api/question/question';
import { QuestionItem } from '@/types/question/question';
import { USE_MOCK } from '@/config/mock';
import { MOCK_QUESTIONS } from '@/mock/questions';
import { normalizeQuestion, normalizeQuestionList } from '@/utils/question/normalize-question';

export function useQuestion() {
  const questions = ref<QuestionItem[]>([]);
  const currentQuestion = ref<QuestionItem | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchQuestions(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getQuestions(params);
      questions.value = normalizeQuestionList(res.data?.list || []);
      total.value = res.data?.total ?? questions.value.length;
    } catch {
      questions.value = USE_MOCK ? normalizeQuestionList(MOCK_QUESTIONS) : [];
      total.value = questions.value.length;
    } finally {
      loading.value = false;
    }
  }

  async function fetchQuestionDetail(id: number) {
    loading.value = true;
    try {
      const res = await getQuestionDetail(id);
      currentQuestion.value = normalizeQuestion(res.data || {});
    } catch {
      const fallback = USE_MOCK ? MOCK_QUESTIONS.find(q => q.id === id) || null : null;
      currentQuestion.value = fallback
        ? normalizeQuestion(fallback)
        : null;
    } finally {
      loading.value = false;
    }
    return currentQuestion.value;
  }

  async function saveQuestion(data: Partial<QuestionItem>, id?: number) {
    if (id) {
      await updateQuestion(id, data);
    } else {
      await createQuestion(data);
    }
  }

  async function removeQuestion(id: number) {
    await deleteQuestion(id);
    questions.value = questions.value.filter(q => q.id !== id);
    total.value = questions.value.length;
  }

  return {
    questions,
    currentQuestion,
    loading,
    total,
    fetchQuestions,
    fetchQuestionDetail,
    saveQuestion,
    removeQuestion
  };
}
