import type { RecommendationQuestion, RecommendationResource } from '@/types/learning/recommendation';
import type { RecommendationItem } from '@/types/learning/recommendation';

const DIFFICULTY_LABELS: Record<string, string> = {
  EASY: '基础巩固',
  MEDIUM: '中等难度',
  HARD: '较难拓展'
};

function difficultyLabel(difficulty?: string): string {
  return DIFFICULTY_LABELS[difficulty || 'MEDIUM'] || '中等难度';
}

export function mapQuestionRecommendation(item: RecommendationQuestion, index: number): RecommendationItem {
  return {
    id: `q-${item.questionId}-${index}`,
    title: item.stem,
    type: 'exercise',
    typeLabel: '推荐练习',
    category: '核心必刷',
    matchScore: Math.max(70, 95 - index * 3),
    courseName: '当前课程',
    knowledgePoint: item.knowledgePointName || '综合考点',
    difficulty: (item.difficulty as RecommendationItem['difficulty']) || 'MEDIUM',
    difficultyLabel: difficultyLabel(item.difficulty),
    estimatedMinutes: 15,
    description: item.reason || '基于近期学习轨迹的 AI 推荐题目。',
    tags: [item.type, item.difficulty].filter(Boolean),
    exerciseMeta: {
      questionCount: 1,
      averageAccuracy: '—'
    }
  };
}

export function mapResourceRecommendation(item: RecommendationResource, index: number): RecommendationItem {
  return {
    id: `r-${item.resourceId || item.documentId || index}`,
    title: item.title,
    type: 'resource',
    typeLabel: item.resourceType || '学习资料',
    category: '精选课件',
    matchScore: Math.max(68, 92 - index * 4),
    courseName: '当前课程',
    knowledgePoint: '拓展资料',
    difficulty: 'EASY',
    difficultyLabel: difficultyLabel('EASY'),
    estimatedMinutes: 10,
    description: item.reason || 'AI 匹配的学习资料推荐。',
    tags: [item.resourceType || '资料'].filter(Boolean),
    resourceMeta: {
      format: item.resourceType || '文档',
      fileSize: '—'
    }
  };
}

export function mergeRecommendations(
  questions: RecommendationQuestion[],
  resources: RecommendationResource[]
): RecommendationItem[] {
  const exerciseItems = questions.map(mapQuestionRecommendation);
  const resourceItems = resources.map(mapResourceRecommendation);
  return [...exerciseItems, ...resourceItems];
}
