import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getWrongBook,
  getWrongBookOverview,
  diagnoseWrongBookItem,
  generateWrongBookVariants,
  masterWrongBookItem
} from '@/api/learning/wrong-book';
import { parseQuestionOptions } from '@/utils/question/normalize-question';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import type { HttpRequestConfig } from '@/core/http/types';
import type {
  WrongBookOverviewVO,
  WrongBookVariantSummary,
  WrongQuestionRecordItem
} from '@/types/learning/wrong-question';

const WRONG_TYPE_CODES = 'CONCEPT|LOGIC|CALC|READING';

/**
 * 剥离诊断正文里的英文类型标记。
 * 这些标记（“类型：CONCEPT”“（READING）”“CONCEPT: …”“……核心特性。 CONCEPT”）只是后端提取
 * error_types 的中间产物，属于内部枚举值；历史数据已原样入库，展示层需再兜底清洗一次，
 * 避免向学生暴露机器字段。
 */
export function stripDiagnosisTypeMarker(text?: string | null): string {
  if (!text) {
    return '';
  }
  return text
    .replace(new RegExp(`^\\s*(?:${WRONG_TYPE_CODES})\\s*[:：]\\s*`, 'i'), '')
    .replace(new RegExp(`[（(]\\s*(?:${WRONG_TYPE_CODES})\\s*[)）]`, 'gi'), ' ')
    .replace(new RegExp(`(?:错因)?类型\\s*[:：]?\\s*(?:${WRONG_TYPE_CODES})\\s*[。.；;]?`, 'gi'), ' ')
    .replace(new RegExp(`(?:属于|归为|标记为|判定为|划分为)\\s*(?:${WRONG_TYPE_CODES})\\s*[。.；;]?`, 'gi'), ' ')
    // 句末裸标记：模型按提示词把类型标注在句末（“……等核心特性。 CONCEPT”），
    // 既无「类型：」前缀也无括号包裹，必须单独兜底，否则学生会看到 CONCEPT 这类内部枚举值；
    // 前缀只吃空格/逗号，句末的「。」保留，避免清洗后结论失去句读
    .replace(new RegExp(`[\\s，,、]*(?:${WRONG_TYPE_CODES})\\s*[。.；;]?\\s*$`, 'i'), '')
    .replace(/\s{2,}/g, ' ')
    .replace(/\s+([，,。.；;：:])/g, '$1')
    .replace(/[，,、]\s*(?=[。.；;]|$)/g, '')
    // 剥离标记后若只剩悬空的逗号/分号，统一收尾为句号，避免出现 “……” 这类断裂结尾
    .replace(/[，,、；;]\s*$/g, '。')
    .replace(/。{2,}/g, '。')
    .trim();
}

/** 是否已有可复用的 AI 诊断结论（用于避免重复调用大模型） */
export function hasDiagnosis(item: WrongQuestionRecordItem | null | undefined): boolean {
  return stripDiagnosisTypeMarker(item?.diagnosis).length >= 10;
}

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

  /**
   * AI 认知归因诊断（单次大模型调用）。
   * 已有诊断结论时默认直接复用，避免每次展开面板都重新请求大模型造成长时间等待；
   * 仅当用户显式要求重新诊断（force=true）时才再次调用。
   */
  async function loadDiagnosis(item: WrongQuestionRecordItem, force = false, config?: HttpRequestConfig) {
    if (!force && hasDiagnosis(item)) {
      return;
    }
    try {
      const res = await diagnoseWrongBookItem(item.id, config);
      if (res?.data) {
        const updated = res.data;
        item.diagnosis = updated.diagnosis ?? item.diagnosis;
        item.errorTypes = updated.errorTypes ?? item.errorTypes;
        item.errorTypeLabels = updated.errorTypeLabels ?? item.errorTypeLabels;
        item.variantQuestionIds = updated.variantQuestionIds ?? item.variantQuestionIds;
        // 归因来源必须同步刷新：只更新正文会让面板继续挂着旧的「演示数据 · 非 AI 结论」标记，
        // 出现「正文已是 AI 结论、标签仍说演示数据」的自相矛盾状态。
        item.diagnosisSource = updated.diagnosisSource ?? (updated.diagnosis ? 'AI' : item.diagnosisSource);
      }
    } catch (err: unknown) {
      // 用户主动中止属于预期行为，不再弹出「暂不可用」误导提示
      if (config?.signal?.aborted) {
        return;
      }
      // 优先展示后端业务提示，避免只显示 "Request failed with status code 500"
      ElMessage.warning(resolveApiErrorMessage(err, 'AI 诊断暂不可用，已展示已有归因结论'));
    }
  }

  /**
   * 按需生成同构变式题（大模型生成并落库），返回可直接渲染的变式题摘要。
   * regenerate=true 时后端会真正重新生成并替换上一批，避免「重新生成」按钮点了没反应。
   */
  async function generateVariants(
    item: WrongQuestionRecordItem,
    regenerate = false,
    config?: HttpRequestConfig
  ): Promise<WrongBookVariantSummary[]> {
    const res = await generateWrongBookVariants(item.id, regenerate, config);
    const variants = res?.data ?? [];
    item.variantQuestionIds = variants.map((v) => v.questionId);
    return variants;
  }

  async function markMastered(item: WrongQuestionRecordItem) {
    try {
      await masterWrongBookItem(item.id);
      wrongList.value = wrongList.value.filter((q) => q.id !== item.id);
      totalWrongQuestions.value = Math.max(0, totalWrongQuestions.value - 1);
      await fetchOverview();
      ElMessage.success(`已将题目 ${item.questionId} 标记为已攻克`);
    } catch {
      ElMessage.error('标记失败，请稍后重试');
    }
  }

  function formatQuestionType(type?: string) {
    const map: Record<string, string> = {
      SINGLE_CHOICE: '单选题',
      MULTIPLE_CHOICE: '多选题',
      FILL_BLANK: '填空题',
      SHORT_ANSWER: '简答题',
      CALCULATION: '计算题',
      ESSAY: '解答题',
      TRUE_FALSE: '判断题',
      JUDGEMENT: '判断题'
    };
    return type ? (map[type.toUpperCase()] || type) : '综合题';
  }

  function getDifficultyType(diff?: string) {
    if (diff === 'EASY') return 'success';
    if (diff === 'HARD') return 'danger';
    return 'warning';
  }

  /**
   * 题目选项解析：复用统一解析器，同时兼容后端两种存储格式
   * —— JSON 数组 [{key:"A",content:"…"}] 与键值对象 {"A":"…"}，
   * 并自动修复 LaTeX 反斜杠导致 JSON.parse 失败的问题。
   * （历史实现仅按对象解析，导致选择题被解析成 "0." "1." 这类空选项行）
   */
  function parsedOptions(optionsJson?: string) {
    return parseQuestionOptions(optionsJson).map((opt) => ({
      key: opt.key,
      content: opt.content
    }));
  }

  /** 诊断正文（已剥离 “类型：CONCEPT” 这类机器标记） */
  function displayDiagnosis(item: WrongQuestionRecordItem) {
    return stripDiagnosisTypeMarker(item.diagnosis);
  }

  /**
   * 失分主因标签。
   * 本次作答为空白时不做归因——「作答缺失」被硬套成审题/计算问题会误导复习方向，
   * 历史数据里这类误标同样在此拦截，不展示。
   */
  function displayErrorTags(item: WrongQuestionRecordItem) {
    if (!String(item.studentAnswer ?? '').trim()) {
      return [];
    }
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
    generateVariants,
    markMastered,
    formatQuestionType,
    getDifficultyType,
    parsedOptions,
    displayDiagnosis,
    displayErrorTags
  };
}
