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


