import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse } from '@/types/common/api';
import type {
  TenantListVO,
  TenantDetailVO,
  TenantOverviewStatsVO,
  TenantCreateRequest,
  TenantUpdateRequest,
  CampusVO,
  CampusCreateRequest,
  CampusUpdateRequest,
  TenantSwitchRequest,
  OrganizationNodeVO,
  OrgCreateRequest,
  OrgUpdateRequest,
  OrganizationMemberVO,
  TenantQuotaVO,
  QuotaUpdateRequest,
  OrgQuotaVO,
  OrgQuotaUpdateRequest
} from '@/types/system/tenant';


// ===== 租户基础管理 =====

export function getCurrentTenant(): Promise<ApiResponse<TenantDetailVO>> {
  return get<TenantDetailVO>('/system/tenants/current');
}

export function getAvailableTenants(): Promise<ApiResponse<TenantListVO[]>> {
  return get<TenantListVO[]>('/system/tenants/available');
}

export function switchTenant(data: TenantSwitchRequest): Promise<ApiResponse<{ token: string; tenantId: number }>> {
  return post<{ token: string; tenantId: number }>('/system/tenants/switch', data);
}

export function getTenantOverviewStats(): Promise<ApiResponse<TenantOverviewStatsVO>> {
  return get<TenantOverviewStatsVO>('/system/tenants/stats');
}

export async function pageTenants(params: {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}): Promise<ApiResponse<{ list: TenantListVO[]; total: number }>> {
  const res = await get<any>('/system/tenants', params);
  if (res && res.data) {
    const rawList = Array.isArray(res.data.records)
      ? res.data.records
      : (Array.isArray(res.data.list) ? res.data.list : (Array.isArray(res.data) ? res.data : []));
    const total = typeof res.data.total === 'number' ? res.data.total : rawList.length;
    return {
      ...res,
      data: {
        list: rawList.map((item: any) => ({
          ...item,
          tenantCode: item.tenantCode || item.code
        })),
        total
      }
    };
  }
  return res;
}

export function getTenantDetail(id: number): Promise<ApiResponse<TenantDetailVO>> {
  return get<TenantDetailVO>(`/system/tenants/${id}`);
}

export function createTenant(data: TenantCreateRequest): Promise<ApiResponse<number>> {
  return post<number>('/system/tenants', {
    ...data,
    code: data.code || data.tenantCode
  });
}

export function updateTenant(id: number, data: TenantUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>(`/system/tenants/${id}`, data);
}

export function updateTenantStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return put<void>(`/system/tenants/${id}/status`, undefined, { params: { status } });
}

export function deleteTenant(id: number): Promise<ApiResponse<void>> {
  return del<void>(`/system/tenants/${id}`);
}

// ===== 校区管理 =====

export function listCampuses(tenantId: number): Promise<ApiResponse<CampusVO[]>> {
  return get<CampusVO[]>(`/system/tenants/${tenantId}/campuses`);
}

export function createCampus(tenantId: number, data: CampusCreateRequest): Promise<ApiResponse<number>> {
  return post<number>(`/system/tenants/${tenantId}/campuses`, data);
}

export function updateCampus(tenantId: number, campusId: number, data: CampusUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>(`/system/tenants/${tenantId}/campuses/${campusId}`, data);
}

export function updateCampusStatus(tenantId: number, campusId: number, status: number): Promise<ApiResponse<void>> {
  return put<void>(`/system/tenants/${tenantId}/campuses/${campusId}/status`, undefined, { params: { status } });
}

export function deleteCampus(tenantId: number, campusId: number): Promise<ApiResponse<void>> {
  return del<void>(`/system/tenants/${tenantId}/campuses/${campusId}`);
}

// ===== 组织架构与班级 =====

export function getOrgTree(tenantId?: number): Promise<ApiResponse<OrganizationNodeVO[]>> {
  return get<OrganizationNodeVO[]>('/system/organizations/tree', { tenantId });
}

export function getTenantOrgStats(tenantId?: number): Promise<ApiResponse<import('@/types/system/tenant').OrgStatsVO>> {
  return get<import('@/types/system/tenant').OrgStatsVO>('/system/organizations/stats', { tenantId });
}

export function getOrgNodeStats(id: number): Promise<ApiResponse<import('@/types/system/tenant').OrgNodeStatsVO>> {
  return get<import('@/types/system/tenant').OrgNodeStatsVO>(`/system/organizations/${id}/stats`);
}

export function getOrgCandidates(id: number, keyword?: string): Promise<ApiResponse<import('@/types/system/tenant').TenantMemberCandidateVO[]>> {
  return get<import('@/types/system/tenant').TenantMemberCandidateVO[]>(`/system/organizations/${id}/candidates`, { keyword });
}

export function batchAssignOrgMembers(id: number, data: import('@/types/system/tenant').OrgMemberBatchAssignRequest): Promise<ApiResponse<void>> {
  return post<void>(`/system/organizations/${id}/members/batch`, data);
}

export function getStudentCognitiveProfile(userId: number): Promise<ApiResponse<import('@/types/system/tenant').StudentCognitiveProfileVO>> {
  return get<import('@/types/system/tenant').StudentCognitiveProfileVO>(`/system/organizations/students/${userId}/profile`);
}

export function createOrgNode(data: OrgCreateRequest): Promise<ApiResponse<number>> {
  return post<number>('/system/organizations', data);
}

export function updateOrgNode(id: number, data: OrgUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>(`/system/organizations/${id}`, data);
}

export function deleteOrgNode(id: number): Promise<ApiResponse<void>> {
  return del<void>(`/system/organizations/${id}`);
}

export function getOrgMembers(id: number): Promise<ApiResponse<OrganizationMemberVO[]>> {
  return get<OrganizationMemberVO[]>(`/system/organizations/${id}/members`, undefined, { silent: true });
}

export function assignOrgMember(id: number, data: { memberId: number; roleType?: string }): Promise<ApiResponse<void>> {
  return post<void>(`/system/organizations/${id}/members`, data);
}

export function removeOrgMember(id: number, memberId: number): Promise<ApiResponse<void>> {
  return del<void>(`/system/organizations/${id}/members/${memberId}`);
}

// ===== 租户配额管控 =====

export function listTenantQuotas(tenantId?: number): Promise<ApiResponse<TenantQuotaVO[]>> {
  return get<TenantQuotaVO[]>('/system/tenant-quotas', { tenantId });
}

export function updateTenantQuota(data: QuotaUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>('/system/tenant-quotas', data);
}

export function listOrgQuotas(tenantId?: number): Promise<ApiResponse<OrgQuotaVO[]>> {
  return get<OrgQuotaVO[]>('/system/tenant-quotas/organizations', { tenantId });
}

export function updateOrgQuota(data: OrgQuotaUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>('/system/tenant-quotas/organizations', data);
}

