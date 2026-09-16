export interface MemoryItemVO {
  id: number;
  namespaceId?: number;
  summary: string;
  fullContent?: string;
  memoryType: 'PREFERENCE' | 'PROFILE' | 'EPISODIC' | 'FEEDBACK' | string;
  sensitivityLevel?: 'NORMAL' | 'ACADEMIC' | 'HIGH_RISK' | string;
  vectorRef?: string;
  confidenceScore?: number;
  encrypted?: boolean;
  keyVersion?: number;
  accessCount?: number;
  sourceChannel?: 'AI_CONVERSATION' | 'MANUAL_INJECTION' | 'DIAGNOSTIC_ANALYSIS' | 'STUDENT_FEEDBACK' | string;
  sourceRef?: string;
  reasoning?: string;
  isNewlyCreated?: boolean;
  createTime?: string;

  // 兼容辅助显示别名
  memoryKey?: string;
  memoryValue?: string;
}

export interface MemorySpaceItemVO {
  namespaceId: number;
  courseId?: number;
  courseTitle: string;
  scope: 'GLOBAL' | 'COURSE' | string;
  consentGranted: boolean;
  retentionDays: number;
  itemCount: number;
}

export interface MemoryOverviewVO {
  totalMemories: number;
  preferenceCount: number;
  profileCount: number;
  episodicCount: number;
  feedbackCount: number;
  encryptedCount: number;
  globalConsentGranted: boolean;
  spaces: MemorySpaceItemVO[];
}

export interface MemoryDecryptVO {
  id: number;
  decryptedContent: string;
  keyVersion: number;
  algorithm: string;
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

export interface MemoryItemUpdateRequest {
  summary: string;
  memoryType?: string;
  sensitivityLevel?: string;
  fullContent?: string;
}

export interface MemoryFeedbackRequest {
  feedbackAction?: 'FORGET' | 'MODIFY' | string;
  correctContent?: string;
  reason?: string;
  relevanceScore?: number; // 1 to 5
  comment?: string;
}

