import { post } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
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
  // AI 选题可能触发大模型原创命题，需放宽超时（默认 30 秒会超时）
  return post<AiPracticeSessionVO>('/learning/ai-practice/start', data, {
    timeout: AI_REQUEST_TIMEOUT
  });
}

/** AI 批改单题；支持传入 AbortSignal，配合推演面板的「中止」按钮真正取消本次等待 */
export function gradeAiPracticeAnswer(data: AiPracticeGradeRequest, config?: HttpRequestConfig) {
  return post<AiPracticeGradeVO>('/learning/ai-practice/grade', data, {
    timeout: AI_REQUEST_TIMEOUT,
    ...config
  });
}

export function submitAiPractice(data: AiPracticeSubmitRequest) {
  // 交卷时会汇总 AI 评阅结果，耗时较长
  return post<AiPracticeSubmitVO>('/learning/ai-practice/submit', data, {
    timeout: AI_REQUEST_TIMEOUT
  });
}
