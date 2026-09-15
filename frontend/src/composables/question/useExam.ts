import { ref } from 'vue';
import { getExams, getExamDetail, createExam, updateExam, deleteExam, exportExam } from '@/api/question/exam';
import { ExamPaper } from '@/types/question/exam';
import { normalizeExamList, normalizeExamPaper } from '@/utils/question/normalize-exam';

export function useExam() {
  const exams = ref<ExamPaper[]>([]);
  const currentExam = ref<ExamPaper | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchExams(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getExams(params);
      exams.value = normalizeExamList((res.data?.list || []) as Record<string, any>[]);
      total.value = res.data?.total ?? exams.value.length;
    } catch (err) {
      exams.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchExamDetail(id: number) {
    loading.value = true;
    try {
      const res = await getExamDetail(id);
      currentExam.value = normalizeExamPaper((res.data || {}) as Record<string, any>);
    } catch (err) {
      currentExam.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentExam.value;
  }

  async function saveExam(data: Partial<ExamPaper>, id?: number) {
    if (id) await updateExam(id, data);
    else await createExam(data);
  }

  async function removeExam(id: number) {
    await deleteExam(id);
    exams.value = exams.value.filter(e => e.id !== id);
    total.value = exams.value.length;
  }

  async function fetchExport(id: number) {
    const res = await exportExam(id);
    return res.data;
  }

  return {
    exams,
    currentExam,
    loading,
    total,
    fetchExams,
    fetchExamDetail,
    saveExam,
    removeExam,
    fetchExport
  };
}
