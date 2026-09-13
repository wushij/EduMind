import { get, post, put } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  OcrTaskVO,
  OcrPageVO,
  OcrTaskCreateRequest,
  OcrPageUpdateRequest
} from '@/types/knowledge/ocr';

export function createOcrTask(data: OcrTaskCreateRequest): Promise<ApiResponse<OcrTaskVO>> {
  return post<OcrTaskVO>('/knowledge/ocr-tasks', data);
}

export function getOcrTaskStatus(taskId: number): Promise<ApiResponse<OcrTaskVO>> {
  return get<OcrTaskVO>(`/knowledge/ocr-tasks/${taskId}`);
}

export function getOcrTaskPages(taskId: number): Promise<ApiResponse<OcrPageVO[]>> {
  return get<OcrPageVO[]>(`/knowledge/ocr-tasks/${taskId}/pages`);
}

export function updateOcrPageText(pageId: number, data: OcrPageUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>(`/knowledge/ocr-tasks/pages/${pageId}`, data);
}

export function confirmOcrTask(taskId: number): Promise<ApiResponse<void>> {
  return post<void>(`/knowledge/ocr-tasks/${taskId}/confirm`);
}
