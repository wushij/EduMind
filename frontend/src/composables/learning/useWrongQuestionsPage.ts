import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { useWrongQuestions } from '@/composables/learning/useWrongQuestions';
import type { WrongQuestionRecordItem } from '@/types/learning/wrong-question';

export function useWrongQuestionsPage(defaultCourseId = 102) {
  const router = useRouter();
  const { courseOptions, courseId: teacherCourseId } = useTeacherCourses(defaultCourseId);

  const {
    courseId,
    loading,
    page,
    pageSize,
    totalWrongQuestions,
    weakPointCount,
    masteredCount,
    selectedErrorType,
    wrongList,
    fetchList,
    loadDiagnosis,
    markMastered,
    formatQuestionType,
    getDifficultyType,
    parsedOptions
  } = useWrongQuestions(teacherCourseId.value);

  const drawerVisible = ref(false);
  const activeItem = ref<WrongQuestionRecordItem | null>(null);

  const handleCourseChange = () => {
    courseId.value = teacherCourseId.value;
    page.value = 1;
    fetchList();
  };

  watch(teacherCourseId, (id) => {
    courseId.value = id;
    fetchList();
  });

  const openDiagnosisDrawer = async (item: WrongQuestionRecordItem) => {
    activeItem.value = item;
    drawerVisible.value = true;
    await loadDiagnosis(item);
  };

  const handleMarkMastered = (item: WrongQuestionRecordItem) => {
    markMastered(item);
  };

  const handleStartVariantPractice = (item: WrongQuestionRecordItem) => {
    router.push({
      path: '/learning/practice',
      query: {
        courseId: courseId.value,
        questionId: item.questionId,
        mode: 'VARIANT'
      }
    });
  };

  const handleLaunchBatchPractice = () => {
    router.push({
      path: '/learning/practice',
      query: {
        courseId: courseId.value,
        mode: 'WRONG_BATCH'
      }
    });
  };

  const handlePracticeSingleVariant = (varId: number) => {
    drawerVisible.value = false;
    router.push({
      path: '/learning/practice',
      query: {
        courseId: courseId.value,
        questionId: varId,
        mode: 'SINGLE_VARIANT'
      }
    });
  };

  const handleLaunchPracticeFromDrawer = () => {
    drawerVisible.value = false;
    if (activeItem.value) {
      handleStartVariantPractice(activeItem.value);
    }
  };

  const handleGoToPractice = () => {
    router.push({
      path: '/learning/practice',
      query: { courseId: courseId.value }
    });
  };

  onMounted(() => {
    courseId.value = teacherCourseId.value;
    fetchList();
  });

  return {
    courseOptions,
    teacherCourseId,
    courseId,
    loading,
    page,
    pageSize,
    totalWrongQuestions,
    weakPointCount,
    masteredCount,
    selectedErrorType,
    wrongList,
    drawerVisible,
    activeItem,
    fetchList,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    handleCourseChange,
    openDiagnosisDrawer,
    handleMarkMastered,
    handleStartVariantPractice,
    handleLaunchBatchPractice,
    handlePracticeSingleVariant,
    handleLaunchPracticeFromDrawer,
    handleGoToPractice
  };
}
