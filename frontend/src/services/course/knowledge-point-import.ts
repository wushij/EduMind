import type { CourseKnowledgePointSuggestItem, KnowledgePoint } from '@/types/course/knowledge-point';

export function resolvePrerequisiteIds(
  titles: string[] | undefined,
  allPoints: KnowledgePoint[]
): number[] {
  if (!titles?.length || !allPoints.length) {
    return [];
  }
  const byTitle = new Map<string, number>();
  for (const kp of allPoints) {
    const t = (kp.title || kp.name || '').trim();
    if (t && kp.id) {
      byTitle.set(t, kp.id);
    }
  }
  const ids: number[] = [];
  for (const raw of titles) {
    const title = raw.trim();
    if (!title) continue;
    const exact = byTitle.get(title);
    if (exact) {
      ids.push(exact);
      continue;
    }
    const fuzzy = allPoints.find(
      (kp) => (kp.title || kp.name || '').includes(title) || title.includes(kp.title || kp.name || '')
    );
    if (fuzzy?.id) {
      ids.push(fuzzy.id);
    }
  }
  return [...new Set(ids)];
}

export function mapSuggestItemToSave(
  item: CourseKnowledgePointSuggestItem,
  chapterId: number,
  prerequisiteIds: number[]
) {
  return {
    chapterId,
    title: item.title,
    description: item.description || '',
    cognitiveDimension: item.cognitiveDimension || 'APPLY',
    importance: item.importance ?? 4,
    examFocus: item.examFocus || '',
    sortOrder: item.importance ?? 4,
    prerequisiteIds
  };
}
