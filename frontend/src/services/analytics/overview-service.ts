import { getAiUsageAnalytics, getLearningAnalytics } from '@/api/analytics/learning';
import { getKnowledgeMastery } from '@/api/analytics/knowledge';
import type { AiUsageAnalyticsVO, LearningAnalyticsVO } from '@/types/analytics/learning';
import type { KnowledgeMasteryVO } from '@/types/analytics/mastery';

export interface OverviewAnalyticsBundle {
  learning: LearningAnalyticsVO | null;
  aiUsage: AiUsageAnalyticsVO | null;
  mastery: KnowledgeMasteryVO | null;
}

export async function fetchOverviewAnalyticsBundle(
  courseId: number,
  range = '30d',
  startDate?: string,
  endDate?: string
): Promise<OverviewAnalyticsBundle> {
  try {
    const [learningRes, aiRes, masteryRes] = await Promise.all([
      getLearningAnalytics({ courseId, range, startDate, endDate }),
      getAiUsageAnalytics({ courseId, range }),
      getKnowledgeMastery({ courseId })
    ]);
    return {
      learning: learningRes.data ?? null,
      aiUsage: aiRes.data ?? null,
      mastery: masteryRes.data ?? null
    };
  } catch {
    return {
      learning: null,
      aiUsage: null,
      mastery: null
    };
  }
}
