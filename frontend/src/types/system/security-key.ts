export interface SecurityKeyVersionVO {
  id: number;
  tenantId: number;
  keyAlias: string;
  keyVersion: number;
  algorithm: string;
  status: 'ACTIVE' | 'DEPRECATED';
  keyFingerprint?: string;
  usageScope?: string;
  activatedTime: string;
  createTime: string;
}

export interface SecurityKeyRotateDTO {
  id?: number;
  keyAlias?: string;
}

export interface SecurityKeyCryptoTestRequest {
  keyAlias?: string;
  keyVersion?: number;
  operation: 'ENCRYPT' | 'DECRYPT';
  text: string;
}

export interface SecurityKeyCryptoTestResponse {
  keyAlias: string;
  keyVersion: number;
  algorithm: string;
  operation: 'ENCRYPT' | 'DECRYPT';
  resultText: string;
  durationMs: number;
  success: boolean;
  message: string;
}

