import { post } from '@/core/http/request';
import type { QuestionItem } from '@/types/question/question';
import type { GenerateQuestionRequest } from '@/types/ai/generation';

export const generateQuestions = (
  params: GenerateQuestionRequest,
  options?: Record<string, unknown>
) =>
  post<QuestionItem[]>('/ai/questions/generate', params, { timeout: 180000, ...options });

/** 中止录题页 AI 生成：释放后端「正在生成中」互斥锁（须与 AbortController 一并调用） */
export const cancelQuestionGenerate = () =>
  post<void>('/ai/questions/generate/cancel', {}, { silent: true });
