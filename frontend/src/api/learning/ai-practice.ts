import { post } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
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

/** AI 批改单题；支持传入 AbortSignal，配合推演面板的「中止」按钮真正取消本次等待 */
export function gradeAiPracticeAnswer(data: AiPracticeGradeRequest, config?: HttpRequestConfig) {
  return post<AiPracticeGradeVO>('/learning/ai-practice/grade', data, config);
}

export function submitAiPractice(data: AiPracticeSubmitRequest) {
  return post<AiPracticeSubmitVO>('/learning/ai-practice/submit', data);
}
