import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getLearningAnalytics, getAiUsageAnalytics, getStudentPortrait } from '@/api/analytics/learning';
import { diagnoseWrongQuestion, getKnowledgeMastery, getWrongQuestions } from '@/api/analytics/knowledge';
import { getTeachingReport } from '@/api/analytics/report';
import { generateTeachingAdvice } from '@/api/analytics/teaching';
import { fetchOverviewAnalyticsBundle } from '@/services/analytics/overview-service';
import { USE_MOCK } from '@/config/mock';
import {
  MOCK_AI_USAGE,
  MOCK_KNOWLEDGE_MASTERY,
  MOCK_LEARNING_ANALYTICS,
  MOCK_STUDENT_PORTRAIT,
  MOCK_TEACHING_ADVICE,
  MOCK_WRONG_QUESTIONS
} from '@/mock/analytics';
import type { AiUsageAnalyticsVO, LearningAnalyticsVO, StudentPortraitVO } from '@/types/analytics/learning';
import type { TeachingReportVO } from '@/types/analytics/report';
import type {
  KnowledgeMasteryVO,
  TeachingAdviceRequest,
  TeachingAdviceVO,
  WrongQuestionAnalyticsVO
} from '@/types/analytics/mastery';

export interface OverviewAnalyticsData {
  learning: LearningAnalyticsVO | null;
  aiUsage: AiUsageAnalyticsVO | null;
  mastery: KnowledgeMasteryVO | null;
}

export interface WrongQuestionDiagnoseResult {
  diagnosis?: string;
  variantQuestionIds: number[];
}

const STORAGE_PREFIX = 'edumind_advice_';

function getStorageKey(courseId: number, studentId?: number | null): string {
  return studentId
    ? `${STORAGE_PREFIX}${courseId}_student_${studentId}`
    : `${STORAGE_PREFIX}${courseId}_overall`;
}

export function getStoredTeachingAdvice(courseId: number, studentId?: number | null): TeachingAdviceVO | null {
  try {
    const raw = localStorage.getItem(getStorageKey(courseId, studentId));
    if (!raw) return null;
    return JSON.parse(raw) as TeachingAdviceVO;
  } catch {
    return null;
  }
}

export function saveStoredTeachingAdvice(
  courseId: number,
  studentId: number | null | undefined,
  advice: TeachingAdviceVO
): void {
  try {
    localStorage.setItem(getStorageKey(courseId, studentId), JSON.stringify(advice));
  } catch {
    // 忽略存储超限异常
  }
}

export function removeStoredTeachingAdvice(courseId: number, studentId?: number | null): void {
  try {
    localStorage.removeItem(getStorageKey(courseId, studentId));
  } catch {
    // 忽略异常
  }
}

export function useLearningAnalytics() {
  const loading = ref(false);
  const usedMockFallback = ref(false);
  const isAggregated = ref(false);
  const learningData = ref<LearningAnalyticsVO | null>(null);
  const portraitData = ref<StudentPortraitVO | null>(null);
  const masteryData = ref<KnowledgeMasteryVO | null>(null);
  const wrongQuestions = ref<WrongQuestionAnalyticsVO | null>(null);
  const aiUsageData = ref<AiUsageAnalyticsVO | null>(null);
  const teachingAdvice = ref<TeachingAdviceVO | null>(null);

  const activeTab = ref<'overall' | 'personal'>('overall');
  const selectedStudentId = ref<number | null>(null);
  const adviceLoading = ref(false);
  let currentAbortController: AbortController | null = null;
  let isManualStopped = false;

  function stopTeachingAdvice() {
    isManualStopped = true;
    if (currentAbortController) {
      try {
        currentAbortController.abort();
      } catch {
        // 忽略
      }
      currentAbortController = null;
    }
    adviceLoading.value = false;
  }

  function clearTeachingAdvice(courseId?: number, studentId?: number | null) {
    stopTeachingAdvice();
    teachingAdvice.value = null;
    if (courseId) {
      removeStoredTeachingAdvice(courseId, studentId);
    }
  }

  function loadStoredAdvice(courseId: number, studentId?: number | null): TeachingAdviceVO | null {
    const stored = getStoredTeachingAdvice(courseId, studentId);
    teachingAdvice.value = stored;
    return stored;
  }

  async function fetchLearning(courseId: number, range = '7d') {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getLearningAnalytics({ courseId, range });
      learningData.value = res.data;
      isAggregated.value = Boolean(res.data?.aggregated);
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        learningData.value = { ...MOCK_LEARNING_ANALYTICS, courseId };
        isAggregated.value = false;
      } else {
        learningData.value = null;
        isAggregated.value = false;
      }
    } finally {
      loading.value = false;
    }

    if (activeTab.value === 'overall') {
      const savedAdvice = getStoredTeachingAdvice(courseId, null);
      if (savedAdvice) {
        teachingAdvice.value = savedAdvice;
      }
    }
  }

  async function fetchStudentPortrait(courseId: number, studentId: number, range = '30d') {
    loading.value = true;
    usedMockFallback.value = false;
    selectedStudentId.value = studentId;
    try {
      const res = await getStudentPortrait({ courseId, studentId, range });
      portraitData.value = res.data;
    } catch (err: unknown) {
      portraitData.value = null;
      if (USE_MOCK) {
        usedMockFallback.value = true;
        portraitData.value = {
          ...MOCK_STUDENT_PORTRAIT,
          studentInfo: {
            ...MOCK_STUDENT_PORTRAIT.studentInfo,
            studentId,
            username: `student_${studentId}`,
            realName: studentId === 3 ? '李同学' : (studentId === 4 ? '王同学' : `学员 ${studentId}`),
            studentNo: `STU-000${studentId}`
          }
        };
      } else {
        const msg = err instanceof Error ? err.message : '加载学员学情画像失败';
        ElMessage.warning(msg);
      }
    } finally {
      loading.value = false;
    }

    const savedAdvice = getStoredTeachingAdvice(courseId, studentId);
    if (savedAdvice) {
      teachingAdvice.value = savedAdvice;
      if (portraitData.value && !portraitData.value.aiDiagnosis) {
        portraitData.value.aiDiagnosis = savedAdvice.summary;
      }
    } else if (portraitData.value?.aiDiagnosis) {
      teachingAdvice.value = {
        summary: portraitData.value.aiDiagnosis,
        actions: ['依据诊断评语安排针对性考点加固', '安排错题定向回溯与针对性微练']
      };
    } else {
      teachingAdvice.value = null;
    }
  }

  async function fetchMastery(courseId: number, studentId?: number) {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getKnowledgeMastery({ courseId, studentId });
      masteryData.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        masteryData.value = MOCK_KNOWLEDGE_MASTERY;
      } else {
        masteryData.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  async function fetchWrongQuestions(
    courseId: number,
    page = 1,
    pageSize = 10,
    knowledgePointId?: number
  ) {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getWrongQuestions({ courseId, page, pageSize, knowledgePointId });
      wrongQuestions.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        wrongQuestions.value = MOCK_WRONG_QUESTIONS;
      } else {
        wrongQuestions.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  async function fetchAiUsage(courseId?: number, range = '7d') {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await getAiUsageAnalytics({ courseId, range });
      aiUsageData.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        aiUsageData.value = MOCK_AI_USAGE;
      } else {
        aiUsageData.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  async function fetchOverview(courseId: number, range = '30d'): Promise<OverviewAnalyticsData> {
    return fetchOverviewAnalyticsBundle(courseId, range);
  }

  async function diagnoseWrong(recordId: number): Promise<WrongQuestionDiagnoseResult | null> {
    try {
      const res = await diagnoseWrongQuestion(recordId);
      const variantIds = res.data?.variantQuestionIds?.split(',').filter(Boolean) ?? [];
      return {
        diagnosis: res.data?.diagnosis,
        variantQuestionIds: variantIds.map((id) => Number(id))
      };
    } catch {
      return null;
    }
  }

  async function fetchTeachingReport(courseId: number): Promise<TeachingReportVO | null> {
    try {
      const res = await getTeachingReport(courseId);
      return res?.data ?? null;
    } catch {
      return null;
    }
  }

  async function fetchTeachingAdvice(request: TeachingAdviceRequest): Promise<TeachingAdviceVO | null> {
    isManualStopped = false;
    stopTeachingAdvice();
    isManualStopped = false;
    currentAbortController = new AbortController();
    adviceLoading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await generateTeachingAdvice(request, {
        signal: currentAbortController.signal
      });
      if (isManualStopped) {
        adviceLoading.value = false;
        return null;
      }
      if (res?.data) {
        teachingAdvice.value = res.data;
        saveStoredTeachingAdvice(request.courseId, request.studentId, res.data);
      }
      return res?.data ?? null;
    } catch (err: any) {
      if (
        isManualStopped ||
        err?.name === 'CanceledError' ||
        err?.name === 'AbortError' ||
        err?.code === 'ERR_CANCELED' ||
        err?.message === 'canceled'
      ) {
        // 用户手动暂停/停止推演
        adviceLoading.value = false;
        return null;
      }
      if (USE_MOCK) {
        usedMockFallback.value = true;
        let mockRes: TeachingAdviceVO;
        if (request.studentId) {
          mockRes = {
            summary: `针对学员 #${request.studentId} 的学情诊断：知识点整体掌握度良好，但部分进阶推演环节存在思维定式，需强化变式应用。`,
            actions: [
              '推送关联薄弱考点的 5 题定向自适应靶向微练',
              '安排参与课后答疑互助或重温考点精讲短视频',
              '要求完成最近错题的归因重做与反思笔记'
            ]
          };
        } else {
          mockRes = MOCK_TEACHING_ADVICE;
        }
        teachingAdvice.value = mockRes;
        saveStoredTeachingAdvice(request.courseId, request.studentId, mockRes);
        return mockRes;
      } else {
        teachingAdvice.value = null;
        return null;
      }
    } finally {
      adviceLoading.value = false;
      currentAbortController = null;
    }
  }

  return {
    loading,
    adviceLoading,
    usedMockFallback,
    isAggregated,
    learningData,
    portraitData,
    masteryData,
    wrongQuestions,
    aiUsageData,
    teachingAdvice,
    activeTab,
    selectedStudentId,
    fetchLearning,
    fetchStudentPortrait,
    fetchMastery,
    fetchWrongQuestions,
    fetchAiUsage,
    fetchOverview,
    diagnoseWrong,
    fetchTeachingReport,
    fetchTeachingAdvice,
    clearTeachingAdvice,
    stopTeachingAdvice,
    loadStoredAdvice
  };
}
