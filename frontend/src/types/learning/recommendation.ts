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
  /** 题目 ID：用于「助教答疑」锚定到侧边栏 AI；资料类推荐为空 */
  questionId?: number;
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
    /** 全班均正答率：后端推荐 VO 暂未提供该统计，无数据时整行不展示，避免出现 “—” 占位 */
    averageAccuracy?: string;
  };
  resourceMeta?: {
    format: string;
    /** 文件大小：后端推荐 VO 暂未返回，无数据时不做占位 */
    fileSize?: string;
  };
  courseId?: number;
}
