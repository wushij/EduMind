import { post } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
import type { HttpRequestConfig } from '@/core/http/types';
import type { TeachingAdviceRequest, TeachingAdviceVO } from '@/types/analytics/mastery';

/**
 * 教学诊断建议生成（服务端同步调用大模型，耗时可达数十秒）。
 *
 * 默认 30 秒超时会在模型还在推演时就把请求掐断，用户只看到弹窗空转，
 * 因此这里与后端 `ai.llm.timeout-ms`（180 秒）对齐，并把调用方的覆盖项放到最后。
 */
export const generateTeachingAdvice = (data: TeachingAdviceRequest, config?: HttpRequestConfig) =>
  post<TeachingAdviceVO>('/analytics/teaching-advice', data, {
    timeout: AI_REQUEST_TIMEOUT,
    ...config
  });
