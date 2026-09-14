export interface SecurityKeyVersionVO {
  id: number;
  tenantId: number;
  keyAlias: string;
  keyVersion: number;
  algorithm: string;
  status: 'ACTIVE' | 'DEPRECATED';
  activatedTime: string;
  createTime: string;
}

export interface SecurityKeyRotateDTO {
  id?: number;
  keyAlias?: string;
}
