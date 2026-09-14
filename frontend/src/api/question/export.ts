import { get, post } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type { PaperExportRequest, ExportTaskVO } from '@/types/question/export';

export function createPaperExportTask(data: PaperExportRequest): Promise<ApiResponse<ExportTaskVO>> {
  return post<ExportTaskVO>('/question/exports/paper', data);
}

export function getExportTaskStatus(taskId: string): Promise<ApiResponse<ExportTaskVO>> {
  return get<ExportTaskVO>(`/question/exports/${taskId}`);
}

export function listMyExportTasks(): Promise<ApiResponse<ExportTaskVO[]>> {
  return get<ExportTaskVO[]>('/question/exports/my');
}
