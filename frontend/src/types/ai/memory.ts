export interface MemoryItemVO {
  id: number;
  namespaceId?: number;
  summary: string;
  fullContent?: string;
  memoryType: 'PREFERENCE' | 'PROFILE' | 'EPISODIC' | 'FEEDBACK' | string;
  sensitivityLevel?: string;
  vectorRef?: string;
  confidenceScore?: number;
  encrypted?: boolean;
  accessCount?: number;
  createTime?: string;

  // 兼容辅助显示别名
  memoryKey?: string;
  memoryValue?: string;
}

export interface MemoryNamespaceVO {
  id: number;
  tenantId: number;
  userId: number;
  courseId?: number;
  scope?: string;
  consentStatus?: boolean;
  consentGranted: boolean;
  retentionDays: number;
  items: MemoryItemVO[];
}

export interface MemoryConsentRequest {
  courseId?: number;
  consentGranted: boolean;
  retentionDays?: number;
  consent?: boolean;
}

export interface MemoryItemCreateRequest {
  courseId?: number;
  summary: string;
  fullContent?: string;
  memoryType?: 'PREFERENCE' | 'PROFILE' | 'EPISODIC' | 'FEEDBACK' | string;
  sensitivityLevel?: 'NORMAL' | 'ACADEMIC' | 'HIGH_RISK' | string;

  // 兼容旧表单输入别名
  memoryKey?: string;
  memoryValue?: string;
}

export interface MemoryFeedbackRequest {
  feedbackAction?: 'FORGET' | 'MODIFY' | string;
  correctContent?: string;
  reason?: string;
  relevanceScore?: number; // 1 to 5
  comment?: string;
}
