import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  MemoryNamespaceVO,
  MemoryItemVO,
  MemoryConsentRequest,
  MemoryItemCreateRequest,
  MemoryFeedbackRequest
} from '@/types/ai/memory';

export function getMemoryNamespace(courseId?: number): Promise<ApiResponse<MemoryNamespaceVO>> {
  return get<MemoryNamespaceVO>('/ai/memories', { courseId });
}

export function updateMemoryConsent(data: MemoryConsentRequest): Promise<ApiResponse<void>> {
  return post<void>('/ai/memories/consent', data);
}

export function createMemoryItem(data: MemoryItemCreateRequest): Promise<ApiResponse<number>> {
  return post<number>('/ai/memories', data);
}

export function forgetMemoryItem(id: number): Promise<ApiResponse<void>> {
  return del<void>(`/ai/memories/${id}`);
}

export function feedbackMemoryItem(id: number, data: MemoryFeedbackRequest): Promise<ApiResponse<void>> {
  return put<void>(`/ai/memories/${id}/feedback`, data);
}

export function retrieveMemories(queryPrompt: string, courseId?: number): Promise<ApiResponse<MemoryItemVO[]>> {
  return get<MemoryItemVO[]>('/ai/memories/retrieve', { queryPrompt, courseId });
}
