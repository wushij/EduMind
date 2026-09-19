export interface RecommendationQuestion {
  id?: number;
  questionId?: number;
  type: string;
  difficulty: string | number;
  stem: string;
  courseId?: number;
  courseName?: string;
  knowledgePointId?: number;
  knowledgePointName?: string;
  matchScore?: number;
  reason?: string;
}

export interface RecommendationResource {
  id?: number;
  resourceId?: number;
  documentId?: number;
  title: string;
  resourceType?: string;
  courseId?: number;
  courseName?: string;
  matchScore?: number;
  reason?: string;
  fileUrl?: string;
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
  courseId?: number;
}
