import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import { getCourseList } from '@/api/course/course';
import { courseLabel } from '@/utils/learning/course-label';
import type { Course } from '@/types/course/course';
import {
  getWrongBookDetail,
  cancelWrongBookDiagnosis,
  cancelWrongBookVariants
} from '@/api/learning/wrong-book';
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
    generateVariants,
    markMastered,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayDiagnosis,
    displayErrorTags
  } = useWrongQuestions(defaultCourseId);

  const drawerVisible = ref(false);
  const activeItem = ref<WrongQuestionRecordItem | null>(null);
  const detailLoading = ref(false);
  const diagnosing = ref(false);
  const variantsLoading = ref(false);
  const detailExtra = ref<Pick<WrongBookDetailVO, 'prerequisiteNodes' | 'variantQuestions'>>({});

  /** AI 请求控制器：面板上的「中止」按钮据此真正取消在途的大模型请求 */
  let diagnosisAbortController: AbortController | null = null;
  let variantsAbortController: AbortController | null = null;

  async function loadCourseOptions() {
    coursesLoading.value = true;
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const courses = (res.data?.list ?? []) as Course[];
      if (courses.length > 0) {
        // ⚠️ 后端 Long 以字符串返回（c.id 运行时是 "103"），必须转数字后再与数字型 courseId 比较，
        // 否则 some() 恒为 false，会把路由指定的课程悄悄换成列表第一门课（曾导致"错题归到 java 课程"）
        courseOptions.value = courses.map((c) => ({
          id: Number(c.id),
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

  /**
   * 打开归因抽屉：仅加载已落库的详情数据（毫秒级返回），
   * 打开时不再自动调用大模型，避免每次展开面板都要等待数十秒。
   * AI 诊断与变式题生成改为用户在抽屉内显式触发。
   */
  const openDiagnosisDrawer = async (item: WrongQuestionRecordItem) => {
    activeItem.value = item;
    drawerVisible.value = true;
    detailExtra.value = {};
    detailLoading.value = true;
    try {
      const res = await getWrongBookDetail(item.id);
      if (res?.data) {
        activeItem.value = { ...item, ...res.data };
        detailExtra.value = {
          prerequisiteNodes: res.data.prerequisiteNodes ?? [],
          variantQuestions: res.data.variantQuestions ?? []
        };
      }
    } catch (err: unknown) {
      ElMessage.warning(err instanceof Error ? err.message : '归因详情加载失败，已展示已有诊断摘要');
    } finally {
      detailLoading.value = false;
    }
  };

  /** 中止在途的 AI 归因诊断；notify=false 用于抽屉关闭等静默场景 */
  const abortDiagnosis = (notify = true) => {
    const hadInFlight = Boolean(diagnosisAbortController);
    if (diagnosisAbortController) {
      diagnosisAbortController.abort();
      diagnosisAbortController = null;
    }
    if (hadInFlight && activeItem.value) {
      // 关掉前端等待后必须显式通知服务端丢弃结果（否则模型跑完仍会落库，下次打开突然冒出结论）
      void cancelWrongBookDiagnosis(activeItem.value.id).catch(() => undefined);
    }
    if (diagnosing.value) {
      diagnosing.value = false;
      if (notify) {
        ElMessage.info('已中止本次 AI 归因诊断，服务端将丢弃本次结果');
      }
    }
  };

  /** 中止在途的变式题生成 */
  const abortVariants = (notify = true) => {
    const hadInFlight = Boolean(variantsAbortController);
    if (variantsAbortController) {
      variantsAbortController.abort();
      variantsAbortController = null;
    }
    if (hadInFlight && activeItem.value) {
      // 通知服务端丢弃本次生成，避免中止的变式题仍被写入题库
      void cancelWrongBookVariants(activeItem.value.id).catch(() => undefined);
    }
    if (variantsLoading.value) {
      variantsLoading.value = false;
      if (notify) {
        ElMessage.info('已中止本次变式题生成，服务端将丢弃本次结果');
      }
    }
  };

  const handleAbortDiagnosis = () => abortDiagnosis(true);
  const handleAbortVariants = () => abortVariants(true);

  /** 关闭抽屉时静默取消在途 AI 请求，避免请求悬挂与「已完成」误提示 */
  watch(drawerVisible, (visible) => {
    if (!visible) {
      abortDiagnosis(false);
      abortVariants(false);
    }
  });

  /** 重新执行 AI 认知归因诊断（用户显式触发，单次大模型调用，可随时中止） */
  const handleRegenerateDiagnosis = async () => {
    const target = activeItem.value;
    if (!target || diagnosing.value) {
      return;
    }
    abortDiagnosis(false);
    const controller = new AbortController();
    diagnosisAbortController = controller;
    diagnosing.value = true;
    try {
      await loadDiagnosis(target, true, { signal: controller.signal, silent: true });
      if (controller.signal.aborted) {
        return;
      }
      activeItem.value = { ...target };
      ElMessage.success('AI 认知归因诊断已更新');
    } finally {
      if (diagnosisAbortController === controller) {
        diagnosisAbortController = null;
        diagnosing.value = false;
      }
    }
  };

  /** 按需生成同构变式题（大模型生成并落库，可随时中止） */
  const handleGenerateVariants = async () => {
    const target = activeItem.value;
    if (!target || variantsLoading.value) {
      return;
    }
    abortVariants(false);
    const controller = new AbortController();
    variantsAbortController = controller;
    variantsLoading.value = true;
    // 已有变式题时按钮文案为「重新生成」，需让后端真正重新出题而不是回缓存
    const isRegenerate = (detailExtra.value.variantQuestions?.length ?? 0) > 0;
    try {
      const variants = await generateVariants(
        target,
        isRegenerate,
        { signal: controller.signal, silent: true }
      );
      if (controller.signal.aborted) {
        return;
      }
      detailExtra.value = { ...detailExtra.value, variantQuestions: variants };
      activeItem.value = { ...target };
      if (variants.length > 0) {
        ElMessage.success(
          `${isRegenerate ? '已重新生成' : '已生成'} ${variants.length} 道同构变式题`
        );
      } else {
        ElMessage.info('本次未生成变式题，可稍后重试');
      }
    } catch (err: unknown) {
      if (controller.signal.aborted) {
        return;
      }
      // 优先展示后端业务提示（如「AI 变式题生成失败，请稍后重试」），避免只显示 "Request failed with status code 500"
      ElMessage.error(resolveApiErrorMessage(err, '变式题生成失败，请稍后重试'));
    } finally {
      if (variantsAbortController === controller) {
        variantsAbortController = null;
        variantsLoading.value = false;
      }
    }
  };

  const handleMarkMastered = (item: WrongQuestionRecordItem) => {
    void markMastered(item);
  };

  const handleStartVariantPractice = (item: WrongQuestionRecordItem) => {
    // 已生成变式题时按变式题集开练（否则只练原题，生成结果等于白做）
    // 题目 ID 是雪花 ID，只能用字符串判断，Number() 会丢精度导致后端查不到
    const variantIds = (item.variantQuestionIds ?? [])
      .map((id) => String(id).trim())
      .filter((id) => /^\d+$/.test(id));
    router.push({
      path: '/learning/practice',
      query: variantIds.length
        ? {
            courseId: String(courseId.value),
            questionIds: variantIds.join(','),
            mode: 'VARIANT'
          }
        : {
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

  const handlePracticeSingleVariant = (varId: number | string) => {
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
    diagnosing,
    variantsLoading,
    detailExtra,
    fetchList,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayDiagnosis,
    displayErrorTags,
    handleCourseChange,
    openDiagnosisDrawer,
    handleRegenerateDiagnosis,
    handleAbortDiagnosis,
    handleGenerateVariants,
    handleAbortVariants,
    handleMarkMastered,
    handleStartVariantPractice,
    handleLaunchBatchPractice,
    handlePracticeSingleVariant,
    handleLaunchPracticeFromDrawer,
    handleGoToPractice,
    handleGoToCourseCenter
  };
}
