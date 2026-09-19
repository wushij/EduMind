import { get, post, del } from '@/core/http/request';
import type { ApiResponse, PageResult } from '@/types/common/api';
import type { PaperExportRequest, ExportTaskVO } from '@/types/question/export';

export function createPaperExportTask(data: PaperExportRequest): Promise<ApiResponse<ExportTaskVO>> {
  return post<ExportTaskVO>('/question/exports/paper', data);
}

export function getExportTaskStatus(taskId: string): Promise<ApiResponse<ExportTaskVO>> {
  return get<ExportTaskVO>(`/question/exports/${taskId}`);
}

export function listMyExportTasks(params?: { page?: number; pageSize?: number }) {
  return get<PageResult<ExportTaskVO>>('/question/exports/my', params);
}

export function deleteExportTask(taskId: string): Promise<ApiResponse<void>> {
  return del<void>(`/question/exports/${taskId}`, undefined, { silent: true });
}
