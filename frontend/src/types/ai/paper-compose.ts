import type { QuestionVO } from '@/types/question/question';

export interface SmartPaperComposeRequest {
  courseId: number;
  knowledgePointIds?: number[];
  totalCount?: number;
  excludeIds?: number[];
  typeRatios?: Record<string, number>;
  difficultyDistribution?: Record<string, number>;
  cognitiveLevels?: Record<string, number>;
  totalScore?: number;
}

export interface SmartPaperComposeVO {
  questions: QuestionVO[];
  selectedCount: number;
  distinctKnowledgePointCount: number;
  coverageRate: number;
  totalScore?: number;
  difficultyHistogram?: Record<string, number>;
  typeDistribution?: Record<string, number>;
}
