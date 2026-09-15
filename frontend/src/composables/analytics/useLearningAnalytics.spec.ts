import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useLearningAnalytics } from './useLearningAnalytics';

vi.mock('@/api/analytics/learning', () => ({
  getLearningAnalytics: vi.fn(),
  getAiUsageAnalytics: vi.fn()
}));

vi.mock('@/api/analytics/knowledge', () => ({
  getKnowledgeMastery: vi.fn(),
  getWrongQuestions: vi.fn(),
  diagnoseWrongQuestion: vi.fn()
}));

vi.mock('@/api/analytics/report', () => ({
  getTeachingReport: vi.fn()
}));

vi.mock('@/api/analytics/teaching', () => ({
  generateTeachingAdvice: vi.fn()
}));

vi.mock('@/config/mock', () => ({
  USE_MOCK: false
}));

import { getLearningAnalytics, getAiUsageAnalytics } from '@/api/analytics/learning';
import { diagnoseWrongQuestion, getKnowledgeMastery } from '@/api/analytics/knowledge';
import { getTeachingReport } from '@/api/analytics/report';

describe('useLearningAnalytics extensions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetchOverview aggregates learning, ai usage and mastery data', async () => {
    vi.mocked(getLearningAnalytics).mockResolvedValue({
      data: { courseId: 101, avgScore: 88, studentCount: 30, completionRate: 0.8, avgStudyMinutes: 40, knowledgeMasteryAvg: 0.7, aiUsageCount: 12, trends: { learning: [], score: [] } }
    } as any);
    vi.mocked(getAiUsageAnalytics).mockResolvedValue({
      data: { totalCalls: 20, totalTokens: 1000, daily: [], byProvider: [] }
    } as any);
    vi.mocked(getKnowledgeMastery).mockResolvedValue({
      data: { dimensions: ['A'], classAvg: [80], studentAvg: [] }
    } as any);

    const { fetchOverview } = useLearningAnalytics();
    const result = await fetchOverview(101, '30d');

    expect(result.learning?.avgScore).toBe(88);
    expect(result.aiUsage?.totalCalls).toBe(20);
    expect(result.mastery?.dimensions).toEqual(['A']);
  });

  it('diagnoseWrong maps variant question ids to numbers', async () => {
    vi.mocked(diagnoseWrongQuestion).mockResolvedValue({
      data: { id: 1, diagnosis: '概念混淆', variantQuestionIds: '11,12' }
    } as any);

    const { diagnoseWrong } = useLearningAnalytics();
    const result = await diagnoseWrong(1);

    expect(result).toEqual({
      diagnosis: '概念混淆',
      variantQuestionIds: [11, 12]
    });
  });

  it('fetchTeachingReport returns report data', async () => {
    vi.mocked(getTeachingReport).mockResolvedValue({
      data: { courseId: 101, avgSubmissionRate: 90, knowledgeMasteryAvg: 0.8, aiCallCount: 5, weeklyActivity: [], errorCategories: [], weakPoints: [] }
    } as any);

    const { fetchTeachingReport } = useLearningAnalytics();
    const result = await fetchTeachingReport(101);

    expect(result?.aiCallCount).toBe(5);
  });
});
