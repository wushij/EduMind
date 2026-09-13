import { ref } from 'vue';
import { getLearningAnalytics, getAiUsageAnalytics } from '@/api/analytics/learning';
import { getKnowledgeMastery, getWrongQuestions } from '@/api/analytics/knowledge';
import { generateTeachingAdvice } from '@/api/analytics/teaching';
import { USE_MOCK } from '@/config/mock';
import {
  MOCK_AI_USAGE,
  MOCK_KNOWLEDGE_MASTERY,
  MOCK_LEARNING_ANALYTICS,
  MOCK_TEACHING_ADVICE,
  MOCK_WRONG_QUESTIONS
} from '@/mock/analytics';
import type { AiUsageAnalyticsVO, LearningAnalyticsVO } from '@/types/analytics/learning';
import type {
  KnowledgeMasteryVO,
  TeachingAdviceRequest,
  TeachingAdviceVO,
  WrongQuestionAnalyticsVO
} from '@/types/analytics/mastery';

export function useLearningAnalytics() {
  const loading = ref(false);
  const usedMockFallback = ref(false);
  const isAggregated = ref(false);
  const learningData = ref<LearningAnalyticsVO | null>(null);
  const masteryData = ref<KnowledgeMasteryVO | null>(null);
  const wrongQuestions = ref<WrongQuestionAnalyticsVO | null>(null);
  const aiUsageData = ref<AiUsageAnalyticsVO | null>(null);
  const teachingAdvice = ref<TeachingAdviceVO | null>(null);

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
    masteryData,
    wrongQuestions,
    aiUsageData,
    teachingAdvice,
    fetchLearning,
    fetchMastery,
    fetchWrongQuestions,
    fetchAiUsage,
    fetchTeachingAdvice
  };
}
