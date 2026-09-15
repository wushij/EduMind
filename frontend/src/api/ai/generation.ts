import { post } from '@/core/http/request';
import type { QuestionItem } from '@/types/question/question';
import type { GenerateQuestionRequest } from '@/types/ai/generation';

export const generateQuestions = (params: GenerateQuestionRequest) =>
  post<QuestionItem[]>('/ai/questions/generate', params);
