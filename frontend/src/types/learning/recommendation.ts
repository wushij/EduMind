export interface RecommendationItem {
  id: string;
  title: string;
  type: 'exercise' | 'resource' | 'concept';
  typeLabel: string;
  category: '薄弱巩固' | '核心必刷' | '精选课件' | '拓展进阶';
  matchScore: number;
  courseName: string;
  knowledgePoint: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  difficultyLabel: string;
  estimatedMinutes: number;
  description: string;
  tags: string[];
  completed?: boolean;
  resourceMeta?: {
    format: string;
    fileSize: string;
  };
  exerciseMeta?: {
    questionCount: number;
    averageAccuracy: string;
  };
}
