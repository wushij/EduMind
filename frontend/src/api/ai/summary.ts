import { get, post, put, del } from '@/core/http/request';
import { AI_REQUEST_TIMEOUT } from '@/config';
import type {
  SummaryGenerateRequest,
  SummaryRecord,
  SummaryRecordDetail
} from '@/types/ai/summary';

/** 一次性生成并落库（同步） */
export const generateSummary = (data: SummaryGenerateRequest) =>
  post<SummaryRecord>('/ai/summary', data, { timeout: AI_REQUEST_TIMEOUT });

/** 历史总结列表 */
export const listSummaryRecords = (params?: { courseId?: number; keyword?: string }) =>
  get<SummaryRecord[]>('/ai/summary/records', params);

/** 总结详情（含正文） */
export const getSummaryRecord = (id: number) =>
  get<SummaryRecordDetail>(`/ai/summary/records/${id}`);

/** 重命名总结标题 */
export const renameSummaryRecord = (id: number, title: string) =>
  put<void>(`/ai/summary/records/${id}`, { title });

/** 删除总结记录 */
export const deleteSummaryRecord = (id: number) =>
  del<void>(`/ai/summary/records/${id}`);

/** 中止某次流式生成 */
export const cancelSummaryStream = (streamId: string) =>
  del<void>(`/ai/summary/stream/${encodeURIComponent(streamId)}`);
