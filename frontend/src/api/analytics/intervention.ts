import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  TeachingInterventionVO,
  InterventionOverviewStatsVO,
  InterventionActionRequest,
  InterventionCreateRequest
} from '@/types/analytics/intervention';

export function listInterventions(courseId?: number): Promise<ApiResponse<TeachingInterventionVO[]>> {
  return get<TeachingInterventionVO[]>('/analytics/interventions', { courseId });
}

export function getInterventionOverviewStats(courseId?: number): Promise<ApiResponse<InterventionOverviewStatsVO>> {
  return get<InterventionOverviewStatsVO>('/analytics/interventions/overview-stats', { courseId });
}

export function scanInterventionTrigger(courseId: number): Promise<ApiResponse<TeachingInterventionVO>> {
  return post<TeachingInterventionVO>(`/analytics/interventions/scan-trigger?courseId=${courseId}`);
}

export function createIntervention(data: InterventionCreateRequest): Promise<ApiResponse<TeachingInterventionVO>> {
  return post<TeachingInterventionVO>('/analytics/interventions', data);
}

export function generateAiInterventionProposal(data: {
  courseId: number;
  knowledgePointId?: number;
  triggerType?: string;
}): Promise<ApiResponse<TeachingInterventionVO>> {
  return post<TeachingInterventionVO>('/analytics/interventions/ai-propose', data);
}

export function customizeIntervention(id: number, data: InterventionActionRequest): Promise<ApiResponse<void>> {
  return put<void>(`/analytics/interventions/${id}/customize`, data);
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

export function deleteIntervention(id: number): Promise<ApiResponse<void>> {
  return del<void>(`/analytics/interventions/${id}`);
}
