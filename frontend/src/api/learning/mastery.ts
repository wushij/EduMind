import { getLearningHomeOverview } from '@/api/learning/home';
import type { KnowledgeMastery } from '@/types/learning/mastery';

/** @deprecated 请使用 getLearningHomeOverview 或 getStudentPortrait */
export async function getMasteryRates() {
  const res = await getLearningHomeOverview();
  const weak = res.data?.weakPoints ?? [];
  const mapped: KnowledgeMastery[] = weak.map((wp) => ({
    name: wp.title,
    rate: wp.mastery
  }));
  return { data: mapped };
}
