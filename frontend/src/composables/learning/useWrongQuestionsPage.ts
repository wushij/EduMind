import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getCourseList } from '@/api/course/course';
import { courseLabel } from '@/utils/learning/course-label';
import type { Course } from '@/types/course/course';
import { getWrongBookDetail } from '@/api/learning/wrong-book';
import { useWrongQuestions } from '@/composables/learning/useWrongQuestions';
import type { WrongBookDetailVO, WrongQuestionRecordItem } from '@/types/learning/wrong-question';

export function useWrongQuestionsPage(defaultCourseId = 102) {
  const router = useRouter();
  const route = useRoute();

  const courseOptions = ref<Array<{ id: number; name: string }>>([]);
  const teacherCourseId = ref<number>(defaultCourseId);
  const hasEnrolledCourses = ref(true);
  const coursesLoading = ref(false);

  const {
    courseId,
    loading,
    overviewLoading,
    page,
    pageSize,
    totalWrongQuestions,
    selectedErrorType,
    wrongList,
    overview,
    fetchList,
    loadDiagnosis,
    markMastered,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayErrorTags
  } = useWrongQuestions(defaultCourseId);

  const drawerVisible = ref(false);
  const activeItem = ref<WrongQuestionRecordItem | null>(null);
  const detailLoading = ref(false);
  const detailExtra = ref<Pick<WrongBookDetailVO, 'prerequisiteNodes' | 'variantQuestions'>>({});

  async function loadCourseOptions() {
    coursesLoading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const courses = (res.data?.list ?? []) as Course[];
      if (courses.length > 0) {
        courseOptions.value = courses.map((c) => ({
          id: c.id,
          name: courseLabel(c)
        }));
        hasEnrolledCourses.value = true;
        const queryCid = route.query.courseId ? Number(route.query.courseId) : null;
        if (queryCid && courseOptions.value.some((c) => c.id === queryCid)) {
          teacherCourseId.value = queryCid;
        } else if (!courseOptions.value.some((c) => c.id === teacherCourseId.value)) {
          teacherCourseId.value = courseOptions.value[0].id;
        }
        courseId.value = teacherCourseId.value;
      } else {
        hasEnrolledCourses.value = false;
        courseOptions.value = [];
      }
    } catch {
      hasEnrolledCourses.value = true;
      courseOptions.value = [{ id: defaultCourseId, name: `课程 #${defaultCourseId}` }];
      teacherCourseId.value = defaultCourseId;
      courseId.value = defaultCourseId;
    } finally {
      coursesLoading.value = false;
    }
  }

  const handleCourseChange = () => {
    courseId.value = teacherCourseId.value;
    page.value = 1;
    void fetchList();
  };

  watch(teacherCourseId, (id) => {
    courseId.value = id;
    page.value = 1;
    void fetchList();
  });

  const openDiagnosisDrawer = async (item: WrongQuestionRecordItem) => {
    activeItem.value = item;
    drawerVisible.value = true;
    detailLoading.value = true;
    detailExtra.value = {};
    try {
      await loadDiagnosis(item);
      const res = await getWrongBookDetail(item.id);
      if (res?.data) {
        activeItem.value = { ...item, ...res.data };
        detailExtra.value = {
          prerequisiteNodes: res.data.prerequisiteNodes,
          variantQuestions: res.data.variantQuestions
        };
      }
    } finally {
      detailLoading.value = false;
    }
  };

  const handleMarkMastered = (item: WrongQuestionRecordItem) => {
    void markMastered(item);
  };

  const handleStartVariantPractice = (item: WrongQuestionRecordItem) => {
    router.push({
      path: '/learning/practice',
      query: {
        courseId: String(courseId.value),
        questionId: String(item.questionId),
        mode: 'VARIANT'
      }
    });
  };

  const handleLaunchBatchPractice = () => {
    router.push({
      path: '/learning/practice',
      query: {
        courseId: String(courseId.value),
        mode: 'WRONG_BATCH'
      }
    });
  };

  const handlePracticeSingleVariant = (varId: number) => {
    drawerVisible.value = false;
    router.push({
      path: '/learning/practice',
      query: {
        courseId: String(courseId.value),
        questionId: String(varId),
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
      query: { courseId: String(courseId.value) }
    });
  };

  const handleGoToCourseCenter = () => {
    router.push('/course');
  };

  onMounted(async () => {
    await loadCourseOptions();
    if (hasEnrolledCourses.value) {
      await fetchList();
    }
  });

  return {
    courseOptions,
    teacherCourseId,
    hasEnrolledCourses,
    coursesLoading,
    courseId,
    loading,
    overviewLoading,
    page,
    pageSize,
    totalWrongQuestions,
    selectedErrorType,
    wrongList,
    overview,
    drawerVisible,
    activeItem,
    detailLoading,
    detailExtra,
    fetchList,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayErrorTags,
    handleCourseChange,
    openDiagnosisDrawer,
    handleMarkMastered,
    handleStartVariantPractice,
    handleLaunchBatchPractice,
    handlePracticeSingleVariant,
    handleLaunchPracticeFromDrawer,
    handleGoToPractice,
    handleGoToCourseCenter
  };
}
