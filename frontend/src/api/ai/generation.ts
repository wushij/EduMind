import { post } from '@/core/http/request';
import { QuestionItem } from '@/types/question/question';

export interface GenerateQuestionRequest {
  courseId: number;
  chapterIds?: number[];
  knowledgePointIds?: number[];
  questionTypes?: string[];
  difficulty?: string;
  count?: number;
  scorePerQuestion?: number;
}

export const generateQuestions = (params: GenerateQuestionRequest) =>
  post<QuestionItem[]>('/ai/questions/generate', params);
