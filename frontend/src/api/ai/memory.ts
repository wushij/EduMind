import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  MemoryNamespaceVO,
  MemoryItemVO,
  MemoryConsentRequest,
  MemoryItemCreateRequest,
  MemoryItemUpdateRequest,
  MemoryFeedbackRequest,
  MemoryOverviewVO,
  MemoryDecryptVO
} from '@/types/ai/memory';

export function getMemoryNamespace(courseId?: number): Promise<ApiResponse<MemoryNamespaceVO>> {
  return get<MemoryNamespaceVO>('/ai/memories', { courseId });
}

export function getMemoryOverview(): Promise<ApiResponse<MemoryOverviewVO>> {
  return get<MemoryOverviewVO>('/ai/memories/overview');
}

export function updateMemoryConsent(data: MemoryConsentRequest): Promise<ApiResponse<void>> {
  return post<void>('/ai/memories/consent', data);
}

export function createMemoryItem(data: MemoryItemCreateRequest): Promise<ApiResponse<number>> {
  return post<number>('/ai/memories', data);
}

export function updateMemoryItem(id: number, data: MemoryItemUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>(`/ai/memories/${id}`, data);
}

export function forgetMemoryItem(id: number): Promise<ApiResponse<void>> {
  return del<void>(`/ai/memories/${id}`);
}

export function forgetAllMemories(courseId?: number): Promise<ApiResponse<void>> {
  return del<void>('/ai/memories/all', { courseId });
}

export function feedbackMemoryItem(id: number, data: MemoryFeedbackRequest): Promise<ApiResponse<void>> {
  return put<void>(`/ai/memories/${id}/feedback`, data);
}

export function retrieveMemories(queryPrompt: string, courseId?: number): Promise<ApiResponse<MemoryItemVO[]>> {
  return get<MemoryItemVO[]>('/ai/memories/retrieve', { queryPrompt, courseId });
}

export function seedSampleMemories(courseId?: number): Promise<ApiResponse<number>> {
  return post<number>('/ai/memories/seed', null, { params: { courseId } });
}

export function extractMemories(courseId?: number, signal?: AbortSignal): Promise<ApiResponse<MemoryItemVO[]>> {
  return post<MemoryItemVO[]>('/ai/memories/extract', null, { params: { courseId }, signal });
}

export function decryptMemoryItem(id: number): Promise<ApiResponse<MemoryDecryptVO>> {
  return get<MemoryDecryptVO>(`/ai/memories/${id}/decrypt`);
}

export function cleanupDuplicateMemories(courseId?: number): Promise<ApiResponse<number>> {
  return post<number>('/ai/memories/cleanup-duplicates', null, { params: { courseId } });
}

export function batchConfirmMemories(courseId?: number, candidates?: Partial<MemoryItemVO>[]): Promise<ApiResponse<number>> {
  return post<number>('/ai/memories/batch-confirm', candidates, { params: { courseId } });
}


