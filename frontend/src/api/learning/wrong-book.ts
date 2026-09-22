import { get, post } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
import type { HttpRequestConfig } from '@/core/http/types';
import type {
  WrongBookDetailVO,
  WrongBookListVO,
  WrongBookOverviewVO,
  WrongBookVariantSummary,
  WrongQuestionRecordItem
} from '@/types/learning/wrong-question';

export function getWrongBook(params: {
  courseId: number;
  page?: number;
  pageSize?: number;
  errorType?: string;
  knowledgePointId?: number;
  status?: number;
}, config?: HttpRequestConfig) {
  return get<WrongBookListVO>('/learning/wrong-book', params, config);
}

export function getWrongBookOverview(courseId: number) {
  return get<WrongBookOverviewVO>('/learning/wrong-book/overview', { courseId });
}

export function getWrongBookDetail(id: number) {
  return get<WrongBookDetailVO>(`/learning/wrong-book/${id}`);
}

/**
 * AI 认知归因诊断（单次大模型调用，不含变式题生成）。
 * 支持传入 AbortSignal 以便用户在推演面板上「中止」时真正取消本次模型请求。
 */
export function diagnoseWrongBookItem(id: number, config?: HttpRequestConfig) {
  return post<WrongQuestionRecordItem>(`/learning/wrong-book/${id}/diagnose`, undefined, {
    timeout: AI_REQUEST_TIMEOUT,
    ...config
  });
}

/**
 * 按需生成同构变式题（大模型生成 + 落库，耗时较长，需用户显式触发，同样支持中止）。
 *
 * @param regenerate false=后端已生成过则直接复用；true=重新生成并替换上一批（「重新生成」按钮用）
 */
export function generateWrongBookVariants(
  id: number,
  regenerate = false,
  config?: HttpRequestConfig
) {
  return post<WrongBookVariantSummary[]>(
    `/learning/wrong-book/${id}/variants`,
    undefined,
    { timeout: AI_REQUEST_TIMEOUT, params: { regenerate }, ...config }
  );
}

export function masterWrongBookItem(id: number) {
  return post<void>(`/learning/wrong-book/${id}/master`);
}

/**
 * 中止 AI 归因诊断：通知服务端丢弃本次模型结果、不再落库。
 * 浏览器中止只关闭连接，服务端线程仍会跑完，因此必须显式通知；失败可忽略（不影响交互）。
 */
export function cancelWrongBookDiagnosis(id: number) {
  return post<void>(`/learning/wrong-book/${id}/diagnose/cancel`, undefined, { silent: true });
}

/** 中止变式题生成：通知服务端丢弃本次结果，且不把题目写入题库 */
export function cancelWrongBookVariants(id: number) {
  return post<void>(`/learning/wrong-book/${id}/variants/cancel`, undefined, { silent: true });
}
