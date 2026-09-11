import { ref } from 'vue';
import {
  getQuestionRecommendations,
  getResourceRecommendations,
  RecommendationQuestion,
  RecommendationResource
} from '@/api/learning/recommendation';
import { USE_MOCK } from '@/config/mock';
import { MOCK_RECOMMENDATIONS } from '@/mock/recommendations';
import type { RecommendationItem } from '@/types/learning/recommendation';
import { mergeRecommendations } from '@/utils/learning/map-recommendation';

export function useRecommendations() {
  const questions = ref<RecommendationQuestion[]>([]);
  const resources = ref<RecommendationResource[]>([]);
  const items = ref<RecommendationItem[]>([]);
  const loading = ref(false);
  const usedMockFallback = ref(false);

  async function fetchRecommendations(courseId?: number, chapterId?: number) {
    loading.value = true;
    usedMockFallback.value = false;
    try {
      const [qRes, rRes] = await Promise.all([
        getQuestionRecommendations({ courseId, chapterId, limit: 10 }),
        getResourceRecommendations({ courseId, chapterId, limit: 6 })
      ]);
      questions.value = qRes.data || [];
      resources.value = rRes.data || [];
      items.value = mergeRecommendations(questions.value, resources.value);
    } catch {
      if (USE_MOCK) {
        usedMockFallback.value = true;
        items.value = MOCK_RECOMMENDATIONS;
        questions.value = MOCK_RECOMMENDATIONS.filter((i) => i.type === 'exercise') as any;
        resources.value = MOCK_RECOMMENDATIONS.filter((i) => i.type === 'resource') as any;
      } else {
        items.value = [];
        questions.value = [];
        resources.value = [];
      }
    } finally {
      loading.value = false;
    }
  }

  return { questions, resources, items, loading, usedMockFallback, fetchRecommendations };
}
