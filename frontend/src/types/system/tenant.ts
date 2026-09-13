export interface CampusVO {
  id: number;
  tenantId: number;
  campusCode: string;
  name: string;
  address?: string;
  isMain: boolean;
  status: number;
  createTime?: string;
}

export interface TenantListVO {
  id: number;
  tenantCode: string;
  name: string;
  domain?: string;
  logoUrl?: string;
  adminName?: string;
  adminPhone?: string;
  status: number;
  expireTime?: string;
  campusCount?: number;
  memberCount?: number;
  createTime?: string;
}

export interface TenantDetailVO extends TenantListVO {
  campuses: CampusVO[];
  description?: string;
}

export interface TenantCreateRequest {
  tenantCode: string;
  name: string;
  domain?: string;
  adminName: string;
  adminPhone: string;
  adminPassword?: string;
  expireTime?: string;
}

export interface TenantSwitchRequest {
  targetTenantId: number;
  reason?: string;
}

export interface OrganizationNodeVO {
  id: number;
  tenantId: number;
  campusId?: number;
  name: string;
  orgType: 'CAMPUS' | 'FACULTY' | 'COLLEGE' | 'DEPT' | 'CLASS';
  parentId: number;
  sortOrder: number;
  leaderName?: string;
  studentCount?: number;
  children?: OrganizationNodeVO[];
}

export interface OrgCreateRequest {
  tenantId?: number;
  name: string;
  orgType: string;
  parentId: number;
  sortOrder?: number;
}

export interface OrgUpdateRequest {
  name: string;
  sortOrder?: number;
}

export interface TenantQuotaVO {
  id?: number;
  tenantId: number;
  quotaType: 'TOKEN' | 'STORAGE' | 'CONCURRENCY' | 'QPS';
  limitValue: number;
  usedValue: number;
  warningThreshold: number;
  resetCycle: string;
  lastResetTime?: string;
}

export interface QuotaUpdateRequest {
  tenantId?: number;
  quotaType: string;
  limitValue?: number;
  warningThreshold?: number;
}
