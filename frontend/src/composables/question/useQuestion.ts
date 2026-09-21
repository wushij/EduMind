import { ref } from 'vue';
import {
  getQuestions,
  getQuestionDetail,
  createQuestion,
  updateQuestion,
  deleteQuestion
} from '@/api/question/question';
import { getCourseList } from '@/api/course/course';
import { QuestionItem } from '@/types/question/question';
import type { Course } from '@/types/course/course';
import {
  normalizeQuestion,
  normalizeQuestionList,
  serializeQuestionForApi
} from '@/utils/question/normalize-question';
import { isGarbageQuestionStem } from '@/utils/question/is-garbage-question-stem';
import { normalizeCourseListFromApi, getCourseDisplayName } from '@/utils/course/course-display';

export async function fetchCoursesForForm(pageSize = 50): Promise<Course[]> {
  const res = await getCourseList({ page: 1, pageSize });
  const rawList = res.data?.list || [];
  return rawList.map((c: any) => ({
    ...c,
    id: Number(c.id),
    name: c.name || c.title || `课程 #${c.id}`,
    title: c.name || c.title || `课程 #${c.id}`
  })) as unknown as Course[];
}

export function useQuestion() {
  const questions = ref<QuestionItem[]>([]);
  const currentQuestion = ref<QuestionItem | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchQuestions(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getQuestions(params);
      const list = normalizeQuestionList(res.data?.list || []).filter(
        (q) => !isGarbageQuestionStem(q.stem)
      );
      questions.value = list;
      const apiTotal = Number(res.data?.total);
      total.value = Number.isFinite(apiTotal) && apiTotal >= 0 ? apiTotal : questions.value.length;
    } catch (err) {
      questions.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  /** id 支持字符串：AI 生成题使用雪花 ID，超出 JS 安全整数范围，必须按字符串传递 */
  async function fetchQuestionDetail(id: number | string) {
    loading.value = true;
    try {
      const res = await getQuestionDetail(id);
      currentQuestion.value = normalizeQuestion(res.data || {});
    } catch (err) {
      currentQuestion.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentQuestion.value;
  }

  async function saveQuestion(data: Partial<QuestionItem>, id?: number | string) {
    const payload = serializeQuestionForApi(data);
    if (id) {
      await updateQuestion(id, payload);
      return id;
    }
    const res = await createQuestion(payload);
    return res.data;
  }

  async function removeQuestion(id: number | string) {
    await deleteQuestion(id);
    questions.value = questions.value.filter(q => String(q.id) !== String(id));
    total.value = Math.max(0, total.value - 1);
  }

  async function loadCourseOptions() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 100 });
      const list = res.data?.list || [];
      return [
        { label: '全部课程', value: null as number | null },
        ...list.map((item: any) => ({
          label: item.title || item.name,
          value: item.id as number
        }))
      ];
    } catch {
      return [{ label: '全部课程', value: null as number | null }];
    }
  }

  return {
    questions,
    currentQuestion,
    loading,
    total,
    fetchQuestions,
    fetchQuestionDetail,
    saveQuestion,
    removeQuestion,
    loadCourseOptions
  };
}
