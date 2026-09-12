import type { QuestionVO } from '@/types/question/question';

export interface SmartPaperComposeRequest {
  courseId: number;
  knowledgePointIds?: number[];
  totalCount?: number;
  excludeIds?: number[];
}

export interface SmartPaperComposeVO {
  questions: QuestionVO[];
  selectedCount: number;
  distinctKnowledgePointCount: number;
  coverageRate: number;
}
