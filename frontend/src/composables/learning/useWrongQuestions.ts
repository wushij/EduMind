import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getWrongBook,
  getWrongBookOverview,
  diagnoseWrongBookItem,
  masterWrongBookItem
} from '@/api/learning/wrong-book';
import type { WrongBookOverviewVO, WrongQuestionRecordItem } from '@/types/learning/wrong-question';

export function useWrongQuestions(initialCourseId = 102) {
  const courseId = ref(initialCourseId);
  const loading = ref(false);
  const overviewLoading = ref(false);
  const page = ref(1);
  const pageSize = ref(10);
  const totalWrongQuestions = ref(0);
  const selectedErrorType = ref('');
  const wrongList = ref<WrongQuestionRecordItem[]>([]);
  const loadError = ref<string | null>(null);
  const overview = ref<WrongBookOverviewVO>({
    pendingCount: 0,
    weakKnowledgePointCount: 0,
    masteredCount: 0,
    variantConquerRatePercent: 0
  });

  async function fetchOverview() {
    overviewLoading.value = true;
    try {
      const res = await getWrongBookOverview(courseId.value);
      if (res?.data) {
        overview.value = res.data;
      }
    } catch {
      overview.value = {
        pendingCount: 0,
        weakKnowledgePointCount: 0,
        masteredCount: 0,
        variantConquerRatePercent: 0
      };
    } finally {
      overviewLoading.value = false;
    }
  }

  async function fetchList() {
    loading.value = true;
    loadError.value = null;
    try {
      const res = await getWrongBook({
        courseId: courseId.value,
        page: page.value,
        pageSize: pageSize.value,
        errorType: selectedErrorType.value || undefined,
        status: 0
      });
      const data = res?.data;
      if (data?.list) {
        wrongList.value = data.list;
        totalWrongQuestions.value = data.total ?? data.list.length;
      } else {
        wrongList.value = [];
        totalWrongQuestions.value = 0;
      }
      await fetchOverview();
    } catch (err: unknown) {
      wrongList.value = [];
      totalWrongQuestions.value = 0;
      loadError.value = err instanceof Error ? err.message : '加载错题列表失败';
      ElMessage.error(loadError.value);
    } finally {
      loading.value = false;
    }
  }

  async function loadDiagnosis(item: WrongQuestionRecordItem) {
    if (item.diagnosis && item.diagnosis.length >= 10 && (item.variantQuestionIds?.length ?? 0) > 0) {
      return;
    }
    try {
      const res = await diagnoseWrongBookItem(item.id);
      if (res?.data) {
        item.diagnosis = res.data.diagnosis ?? item.diagnosis;
        item.errorTypes = res.data.errorTypes ?? item.errorTypes;
        item.errorTypeLabels = res.data.errorTypeLabels ?? item.errorTypeLabels;
        item.variantQuestionIds = res.data.variantQuestionIds ?? item.variantQuestionIds;
      }
    } catch {
      ElMessage.warning('诊断接口暂不可用，展示已有摘要');
    }
  }

  async function markMastered(item: WrongQuestionRecordItem) {
    try {
      await masterWrongBookItem(item.id);
      wrongList.value = wrongList.value.filter((q) => q.id !== item.id);
      totalWrongQuestions.value = Math.max(0, totalWrongQuestions.value - 1);
      await fetchOverview();
      ElMessage.success(`已将题目 #${item.questionId} 标记为已攻克`);
    } catch {
      ElMessage.error('标记失败，请稍后重试');
    }
  }

  function formatQuestionType(type?: string) {
    const map: Record<string, string> = {
      SINGLE_CHOICE: '单选题',
      MULTIPLE_CHOICE: '多选题',
      CALCULATION: '计算题',
      ESSAY: '解答题',
      TRUE_FALSE: '判断题'
    };
    return type ? map[type] || '综合题' : '综合题';
  }

  function getDifficultyType(diff?: string) {
    if (diff === 'EASY') return 'success';
    if (diff === 'HARD') return 'danger';
    return 'warning';
  }

  function parsedOptions(optionsJson?: string) {
    if (!optionsJson) return [];
    try {
      const obj = JSON.parse(optionsJson) as Record<string, string>;
      return Object.entries(obj).map(([key, val]) => ({ key, val }));
    } catch {
      return [];
    }
  }

  function displayErrorTags(item: WrongQuestionRecordItem) {
    if (item.errorTypeLabels?.length) {
      return item.errorTypeLabels;
    }
    if (item.errorTypes?.length) {
      return item.errorTypes;
    }
    return [];
  }

  return {
    courseId,
    loading,
    overviewLoading,
    page,
    pageSize,
    totalWrongQuestions,
    selectedErrorType,
    wrongList,
    loadError,
    overview,
    fetchList,
    fetchOverview,
    loadDiagnosis,
    markMastered,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayErrorTags
  };
}
