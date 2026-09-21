import type { RecommendationQuestion, RecommendationResource } from '@/types/learning/recommendation';
import type { RecommendationItem } from '@/types/learning/recommendation';
import type { Question } from '@/types/question/question';

const DIFFICULTY_LABELS: Record<string, string> = {
  EASY: '基础巩固',
  MEDIUM: '中等难度',
  HARD: '较难拓展'
};

const TYPE_LABELS: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  CALCULATION: '计算题',
  ESSAY: '解答题'
};

/** 各题型基准解题耗时（分钟），用于替代原先写死的「预估耗时 15 分钟」 */
const TYPE_BASE_MINUTES: Record<string, number> = {
  SINGLE_CHOICE: 3,
  MULTIPLE_CHOICE: 4,
  TRUE_FALSE: 2,
  FILL_BLANK: 4,
  SHORT_ANSWER: 12,
  CALCULATION: 12,
  ESSAY: 15
};

/** 后端难度为 1~5 的数值，前端展示为 EASY/MEDIUM/HARD；两种入参都需兼容 */
export function normalizeDifficulty(difficulty?: string | number): 'EASY' | 'MEDIUM' | 'HARD' {
  if (typeof difficulty === 'number' && Number.isFinite(difficulty)) {
    if (difficulty <= 2) return 'EASY';
    if (difficulty >= 4) return 'HARD';
    return 'MEDIUM';
  }
  const raw = String(difficulty ?? '').trim();
  const upper = raw.toUpperCase();
  if (upper === 'EASY' || upper === 'MEDIUM' || upper === 'HARD') {
    return upper;
  }
  const num = Number(raw);
  return raw !== '' && Number.isFinite(num) ? normalizeDifficulty(num) : 'MEDIUM';
}

function difficultyLabel(difficulty?: string | number): string {
  return DIFFICULTY_LABELS[normalizeDifficulty(difficulty)] || '中等难度';
}

function typeLabel(type?: string): string {
  return TYPE_LABELS[String(type ?? '').toUpperCase()] || '综合题';
}

/** 按「题型基准耗时 × 难度系数」估算，替代原先所有题目都写死 15 分钟的做法 */
function estimateMinutes(type?: string, difficulty?: string | number): number {
  const base = TYPE_BASE_MINUTES[String(type ?? '').toUpperCase()] ?? 6;
  const level = normalizeDifficulty(difficulty);
  const factor = level === 'HARD' ? 1.4 : level === 'EASY' ? 0.8 : 1;
  return Math.max(2, Math.round(base * factor));
}

/**
 * 分类标签必须与「学习推荐」页的分类 Tab 取值严格一致，
 * 否则 Tab 计数恒为 0（Tab 值：薄弱巩固 / 核心必刷 / 精选课件 / 拓展进阶）。
 */
const CATEGORY_WEAK = '薄弱巩固';
const CATEGORY_MUST = '核心必刷';
const CATEGORY_COURSEWARE = '精选课件';
const CATEGORY_ADVANCED = '拓展进阶';

/** 题目分类：高难题目归「拔高进阶」，匹配度极高归「核心必刷」，其余归「薄弱考点巩固」 */
function categoryForQuestion(matchScore: number, level: 'EASY' | 'MEDIUM' | 'HARD'): string {
  if (level === 'HARD') return CATEGORY_ADVANCED;
  if (matchScore >= 95) return CATEGORY_MUST;
  return CATEGORY_WEAK;
}

/** 资料分类：后端在 reason 中标出「与薄弱章节相关」，据此归入薄弱巩固，其余为精选课件 */
function categoryForResource(reason?: string): string {
  const text = reason ?? '';
  return text.includes('薄弱') ? CATEGORY_WEAK : CATEGORY_COURSEWARE;
}

export function mapQuestionRecommendation(item: RecommendationQuestion, index: number): RecommendationItem {
  const qid = item.questionId ?? item.id ?? index;
  const matchScore = item.matchScore ?? Math.max(70, 95 - index * 3);
  const level = normalizeDifficulty(item.difficulty);
  return {
    id: `q-${qid}-${index}`,
    questionId: item.questionId ?? item.id,
    title: item.stem,
    type: 'exercise',
    typeLabel: '推荐练习',
    courseId: item.courseId,
    category: categoryForQuestion(matchScore, level),
    matchScore,
    courseName: item.courseName || '当前课程',
    knowledgePoint: item.knowledgePointName || '综合考点',
    difficulty: level,
    difficultyLabel: difficultyLabel(item.difficulty),
    estimatedMinutes: estimateMinutes(item.type, item.difficulty),
    description: item.reason || '基于近期学习轨迹的 AI 推荐题目。',
    tags: [typeLabel(item.type), difficultyLabel(item.difficulty)],
    exerciseMeta: {
      questionCount: 1
    }
  };
}

export function mapResourceRecommendation(item: RecommendationResource, index: number): RecommendationItem {
  const matchScore = item.matchScore ?? Math.max(68, 92 - index * 4);
  return {
    id: `r-${item.resourceId || item.id || item.documentId || index}`,
    title: item.title,
    type: 'resource',
    typeLabel: item.resourceType || '学习资料',
    courseId: item.courseId,
    category: categoryForResource(item.reason),
    matchScore,
    courseName: item.courseName || '当前课程',
    knowledgePoint: '拓展资料',
    difficulty: 'EASY',
    difficultyLabel: difficultyLabel('EASY'),
    estimatedMinutes: 10,
    description: item.reason || 'AI 匹配的学习资料推荐。',
    tags: [item.resourceType || '学习资料'],
    resourceMeta: {
      format: item.resourceType || '文档'
    }
  };
}

/**
 * 把推荐项转换为侧边栏 AI 辅导所需的题目上下文。
 * 推荐接口只返回题干与考点，这里补齐 Question 的最小必填字段，
 * 以便复用题库列表那套「AI 辅导」能力（打开侧边栏并锚定该题）。
 */
export function toTutorQuestion(item: RecommendationItem): Question {
  return {
    id: item.questionId ?? 0,
    courseId: item.courseId ?? 0,
    type: 'SINGLE_CHOICE',
    difficulty: item.difficulty,
    score: 0,
    stem: item.title,
    correctAnswer: '',
    analysis: '',
    knowledgePointNames: item.knowledgePoint ? [item.knowledgePoint] : []
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
