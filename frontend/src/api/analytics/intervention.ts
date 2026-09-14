import { get, post } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type { TeachingInterventionVO, InterventionActionRequest, InterventionCreateRequest } from '@/types/analytics/intervention';

export function listInterventions(courseId?: number): Promise<ApiResponse<TeachingInterventionVO[]>> {
  return get<TeachingInterventionVO[]>('/analytics/interventions', { courseId });
}

export function createIntervention(data: InterventionCreateRequest): Promise<ApiResponse<TeachingInterventionVO>> {
  return post<TeachingInterventionVO>('/analytics/interventions', data);
}

export function approveIntervention(id: number, data?: InterventionActionRequest): Promise<ApiResponse<void>> {
  return post<void>(`/analytics/interventions/${id}/approve`, data || {});
}

export function rejectIntervention(id: number): Promise<ApiResponse<void>> {
  return post<void>(`/analytics/interventions/${id}/reject`);
}

export function dispatchIntervention(id: number): Promise<ApiResponse<void>> {
  return post<void>(`/analytics/interventions/${id}/dispatch`);
}
