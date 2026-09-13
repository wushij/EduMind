export interface MemoryItemVO {
  id: number;
  namespaceId: number;
  memoryKey: string;
  memoryValue: string;
  memoryType: 'PREFERENCE' | 'PROFILE' | 'EPISODIC' | 'FEEDBACK';
  confidenceScore: number;
  accessCount: number;
  lastAccessTime?: string;
  createTime?: string;
}

export interface MemoryNamespaceVO {
  id: number;
  tenantId: number;
  userId: number;
  courseId?: number;
  namespaceKey: string;
  consentGranted: boolean;
  retentionDays: number;
  items: MemoryItemVO[];
}

export interface MemoryConsentRequest {
  courseId?: number;
  consentGranted: boolean;
  retentionDays?: number;
}

export interface MemoryItemCreateRequest {
  courseId?: number;
  memoryKey: string;
  memoryValue: string;
  memoryType: string;
  confidenceScore?: number;
}

export interface MemoryFeedbackRequest {
  relevanceScore: number; // 1 to 5
  comment?: string;
}
