import { post } from '@/core/http/request';
import type {
  AiPracticeGradeRequest,
  AiPracticeGradeVO,
  AiPracticeSessionVO,
  AiPracticeStartRequest,
  AiPracticeSubmitRequest,
  AiPracticeSubmitVO
} from '@/types/learning/ai-practice';

export function startAiPractice(data: AiPracticeStartRequest) {
  return post<AiPracticeSessionVO>('/learning/ai-practice/start', data);
}

export function gradeAiPracticeAnswer(data: AiPracticeGradeRequest) {
  return post<AiPracticeGradeVO>('/learning/ai-practice/grade', data);
}

export function submitAiPractice(data: AiPracticeSubmitRequest) {
  return post<AiPracticeSubmitVO>('/learning/ai-practice/submit', data);
}
