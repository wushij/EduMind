import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { getWrongQuestionList, diagnoseWrongQuestion } from '@/api/learning/wrong-question';
import type { WrongQuestionRecordItem } from '@/types/learning/wrong-question';

export function useWrongQuestions(initialCourseId = 102) {
  const courseId = ref(initialCourseId);
  const loading = ref(false);
  const page = ref(1);
  const pageSize = ref(10);
  const totalWrongQuestions = ref(0);
  const selectedErrorType = ref('');
  const wrongList = ref<WrongQuestionRecordItem[]>([]);
  const loadError = ref<string | null>(null);
  const masteredCount = ref(0);

  const weakPointCount = computed(() => {
    const kpSet = new Set<number>();
    wrongList.value.forEach((item) => {
      item.errorTypes?.forEach(() => kpSet.add(item.questionId));
    });
    return kpSet.size;
  });

  async function fetchList() {
    loading.value = true;
    loadError.value = null;
    try {
      const res = await getWrongQuestionList({
        courseId: courseId.value,
        page: page.value,
        pageSize: pageSize.value
      });
      const data = res?.data;
      if (data?.list) {
        wrongList.value = data.list;
        totalWrongQuestions.value = data.total ?? data.list.length;
      } else {
        wrongList.value = [];
        totalWrongQuestions.value = 0;
      }
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
    if (item.diagnosis && item.diagnosis.length >= 10) return;
    try {
      const res = await diagnoseWrongQuestion(item.id);
      if (res?.data) {
        item.diagnosis = res.data.diagnosis ?? item.diagnosis;
        item.errorTypes = res.data.errorTypes ?? item.errorTypes;
        item.variantQuestionIds = res.data.variantQuestionIds ?? item.variantQuestionIds;
      }
    } catch {
      ElMessage.warning('诊断接口暂不可用，展示已有摘要');
    }
  }

  function markMastered(item: WrongQuestionRecordItem) {
    wrongList.value = wrongList.value.filter((q) => q.id !== item.id);
    totalWrongQuestions.value = Math.max(0, totalWrongQuestions.value - 1);
    masteredCount.value += 1;
    ElMessage.success(`已将题目 #${item.questionId} 标记为已攻克！`);
  }

  function handleCourseChange() {
    page.value = 1;
    fetchList();
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

  return {
    courseId,
    loading,
    page,
    pageSize,
    totalWrongQuestions,
    weakPointCount,
    masteredCount,
    selectedErrorType,
    wrongList,
    loadError,
    fetchList,
    loadDiagnosis,
    markMastered,
    handleCourseChange,
    formatQuestionType,
    getDifficultyType,
    parsedOptions
  };
}
