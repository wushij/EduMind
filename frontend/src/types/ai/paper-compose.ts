import type { QuestionVO } from '@/types/question/question';

export interface SmartPaperComposeRequest {
  courseId: number;
  chapterIds?: number[];
  knowledgePointIds?: number[];
  totalCount?: number;
  totalScore?: number;
  excludeIds?: number[];
  excludeQuestionIds?: number[];
  typeRatios?: Record<string, number>;
  difficultyDistribution?: Record<string, number>;
  cognitiveLevels?: Record<string, number>;
  difficultyModel?: 'FOUNDATION' | 'NORMAL' | 'ADVANCED';
  promptDirective?: string;
  aiGenerateFillShortfall?: boolean;
}

export interface SmartPaperComposeVO {
  questions: QuestionVO[];
  selectedCount: number;
  distinctKnowledgePointCount: number;
  coverageRate: number;
  totalScore?: number;
  difficultyHistogram?: Record<string, number>;
  typeDistribution?: Record<string, number>;
  duplicateRate?: number;
  shortfallCount?: number;
  aiGeneratedCount?: number;
  bankExtractedCount?: number;
  examQualityAssessment?: string;
}

export interface SmartPaperSwapRequest {
  courseId: number;
  oldQuestionId: number | string;
  type?: string;
  difficulty?: number;
  score?: number;
  knowledgePointId?: number;
  knowledgePointName?: string;
  excludeQuestionIds?: (number | string)[];
  promptDirective?: string;
}
