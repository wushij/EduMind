export interface RecommendationQuestion {
  questionId: number;
  type: string;
  difficulty: string;
  stem: string;
  knowledgePointName?: string;
  reason?: string;
}

export interface RecommendationResource {
  resourceId?: number;
  documentId?: number;
  title: string;
  resourceType?: string;
  reason?: string;
}

export interface RecommendationItem {
  id: string;
  title: string;
  type: 'exercise' | 'resource';
  typeLabel: string;
  category: string;
  matchScore: number;
  courseName: string;
  knowledgePoint: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  difficultyLabel: string;
  estimatedMinutes: number;
  description: string;
  tags: string[];
  completed?: boolean;
  exerciseMeta?: {
    questionCount: number;
    averageAccuracy: string;
  };
  resourceMeta?: {
    format: string;
    fileSize: string;
  };
}
