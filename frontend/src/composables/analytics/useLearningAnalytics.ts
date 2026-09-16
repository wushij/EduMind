import { ref } from 'vue';
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
  }

  async function fetchStudentPortrait(courseId: number, studentId: number) {
    loading.value = true;
    usedMockFallback.value = false;
    selectedStudentId.value = studentId;
    try {
      const res = await getStudentPortrait({ courseId, studentId });
      portraitData.value = res.data;
    } catch {
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
        portraitData.value = null;
      }
    } finally {
      loading.value = false;
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

  async function fetchTeachingAdvice(request: TeachingAdviceRequest) {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const res = await generateTeachingAdvice(request);
      teachingAdvice.value = res.data;
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        teachingAdvice.value = MOCK_TEACHING_ADVICE;
      } else {
        teachingAdvice.value = null;
      }
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
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
    fetchTeachingAdvice
  };
}
