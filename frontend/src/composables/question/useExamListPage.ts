import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useExam } from '@/composables/question/useExam';
import type { ExamPaper } from '@/types/question/exam';

export function useExamListPage() {
  const router = useRouter();
  const { exams, loading, total, fetchExams, loadCourseOptions } = useExam();

  const pageNum = ref(1);
  const pageSize = ref(10);
  const selectedCourseId = ref<number | null>(null);
  const keyword = ref<string>('');

  const courseOptions = ref<Array<{ label: string; value: number | null }>>([
    { label: '全部课程', value: null }
  ]);

  const pageReady = ref(false);

  async function loadCourseFilterOptions() {
    courseOptions.value = await loadCourseOptions();
  }

  async function loadExams() {
    await fetchExams({
      page: pageNum.value,
      pageSize: pageSize.value,
      courseId: selectedCourseId.value || undefined,
      keyword: keyword.value.trim() || undefined
    });
  }

  function handleCourseFilter(value: number | null) {
    selectedCourseId.value = value;
    pageNum.value = 1;
    loadExams();
  }

  function handleSearch() {
    pageNum.value = 1;
    loadExams();
  }

  function clearKeyword() {
    keyword.value = '';
    pageNum.value = 1;
    loadExams();
  }

  function handlePreview(exam: ExamPaper) {
    router.push(`/question/exams/${exam.id}`);
  }

  function handleExportPdf(exam: ExamPaper) {
    ElMessage.success(`试卷《${exam.title}》已生成排版，准备导出 PDF...`);
  }

  function handlePublish(exam: ExamPaper) {
    ElMessage.success(`试卷《${exam.title}》已成功发布至选课班级考试中心！`);
  }

  onMounted(async () => {
    pageReady.value = false;
    try {
      await Promise.all([loadCourseFilterOptions(), loadExams()]);
    } finally {
      pageReady.value = true;
    }
  });

  return {
    router,
    exams,
    loading,
    total,
    pageNum,
    pageSize,
    selectedCourseId,
    keyword,
    courseOptions,
    pageReady,
    loadExams,
    handleCourseFilter,
    handleSearch,
    clearKeyword,
    handlePreview,
    handleExportPdf,
    handlePublish
  };
}
