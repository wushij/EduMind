export interface CampusVO {
  id: number;
  tenantId: number;
  code?: string;
  campusCode: string;
  name: string;
  address?: string;
  isMain: boolean;
  status: number;
  createTime?: string;
}

export interface TenantOverviewStatsVO {
  totalTenants: number;
  activeTenants: number;
  totalCampuses: number;
  totalMembers: number;
  totalStudents: number;
  totalTeachers: number;
  complianceRate: number;
  totalTokenQuota: number;
  usedTokenQuota: number;
}

export interface TenantListVO {
  id: number;
  code?: string;
  tenantCode: string;
  name: string;
  domain?: string;
  logo?: string;
  logoUrl?: string;
  planCode?: string;
  planName?: string;
  adminName?: string;
  adminPhone?: string;
  status: number;
  expireTime?: string;
  campusCount?: number;
  memberCount?: number;
  studentCount?: number;
  teacherCount?: number;
  tokenUsagePercent?: number;
  storageUsagePercent?: number;
  seatsUsagePercent?: number;
  createTime?: string;
}

export interface TenantDetailVO extends TenantListVO {
  campuses: CampusVO[];
  description?: string;
  quotas?: TenantQuotaVO[];
}

export interface TenantCreateRequest {
  code?: string;
  tenantCode: string;
  name: string;
  domain?: string;
  logo?: string;
  planCode?: string;
  adminName: string;
  adminPhone: string;
  adminPassword?: string;
  expireTime?: string;
}

export interface TenantUpdateRequest {
  name: string;
  logo?: string;
  domain?: string;
  planCode?: string;
  expireTime?: string;
  adminName?: string;
  adminPhone?: string;
  status?: number;
}

export interface CampusCreateRequest {
  code: string;
  name: string;
  address?: string;
  isMain?: boolean;
  status?: number;
}

export interface CampusUpdateRequest {
  name: string;
  address?: string;
  isMain?: boolean;
  status?: number;
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

export interface OrgMemberAssignRequest {
  memberId: number;
  roleType?: string;
}

export interface TenantQuotaVO {
  id?: number;
  tenantId: number;
  quotaType: 'TOKEN' | 'STORAGE' | 'CONCURRENCY' | 'QPS' | 'SEATS';
  limitValue: number;
  usedValue: number;
  usagePercent?: number;
  warningThreshold: number;
  isWarning?: boolean;
  resetCycle?: string;
  lastResetTime?: string;
}

export interface QuotaUpdateRequest {
  tenantId?: number;
  quotaType: string;
  limitValue?: number;
  warningThreshold?: number;
}

export interface OrganizationMemberVO {
  id: number;
  userId: number;
  studentNo: string;
  name: string;
  role: string;
  avatar?: string;
  masteryRate?: number | null;
  lastActive?: string | null;
}

export interface OrgStatsVO {
  campusCount: number;
  facultyCount: number;
  classCount: number;
  studentCount: number;
  teacherCount: number;
}

export interface OrgNodeStatsVO {
  orgId: number;
  orgName: string;
  orgType: string;
  studentCount: number;
  teacherCount: number;
  avgMasteryRate: number;
  homeworkSubmissionRate: number;
  pendingInterventions: number;
}

export interface TenantMemberCandidateVO {
  memberId: number;
  userId: number;
  memberNo: string;
  realName: string;
  username: string;
  avatar?: string;
  phone?: string;
  isAssigned: boolean;
  currentRole?: string;
}

export interface OrgMemberBatchAssignRequest {
  memberIds: number[];
  roleType: string;
}

export interface StudentCognitiveProfileVO {
  userId: number;
  realName: string;
  studentNo: string;
  avatar?: string;
  phone?: string;
  className: string;
  lastActive?: string;
  overallMastery: number;
  assessedCount: number;
  weakKnowledgePoints: string[];
  masteredKnowledgePoints: string[];
  details?: Array<{
    knowledgePointId: number;
    name: string;
    score: number;
    sampleCount: number;
    lastAssessedAt?: string;
  }>;
}

export interface OrgQuotaVO {
  orgId: number;
  name: string;
  orgType: string;
  orgTypeLabel: string;
  campusName: string;
  tokenLimit: number;
  tokenUsed: number;
  usagePercent: number;
  warningThreshold: number;
  storageLimit: number;
  storageUsed: number;
  seatsLimit: number;
  seatsUsed: number;
  status: 'NORMAL' | 'WARNING' | 'EXCEEDED';
}

export interface OrgQuotaUpdateRequest {
  tenantId?: number;
  orgId: number;
  tokenLimit?: number;
  storageLimit?: number;
  seatsLimit?: number;
  warningThreshold?: number;
}
