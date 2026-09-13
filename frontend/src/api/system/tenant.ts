import { get, post, put, del } from '@/core/http/request';
import type { ApiResponse, PageResult } from '@/types/common/api';
import type {
  TenantListVO,
  TenantDetailVO,
  TenantCreateRequest,
  TenantSwitchRequest,
  OrganizationNodeVO,
  OrgCreateRequest,
  OrgUpdateRequest,
  TenantQuotaVO,
  QuotaUpdateRequest
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

export function pageTenants(params: {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}): Promise<ApiResponse<PageResult<TenantListVO>>> {
  return get<PageResult<TenantListVO>>('/system/tenants', params);
}

export function getTenantDetail(id: number): Promise<ApiResponse<TenantDetailVO>> {
  return get<TenantDetailVO>(`/system/tenants/${id}`);
}

export function createTenant(data: TenantCreateRequest): Promise<ApiResponse<number>> {
  return post<number>('/system/tenants', data);
}

// ===== 组织架构与班级 =====

export function getOrgTree(tenantId?: number): Promise<ApiResponse<OrganizationNodeVO[]>> {
  return get<OrganizationNodeVO[]>('/system/organizations/tree', { tenantId });
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

// ===== 租户配额管控 =====

export function listTenantQuotas(tenantId?: number): Promise<ApiResponse<TenantQuotaVO[]>> {
  return get<TenantQuotaVO[]>('/system/tenant-quotas', { tenantId });
}

export function updateTenantQuota(data: QuotaUpdateRequest): Promise<ApiResponse<void>> {
  return put<void>('/system/tenant-quotas', data);
}
