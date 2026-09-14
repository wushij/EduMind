import { get, post } from '@/core/http/request';
import type { SecurityKeyVersionVO, SecurityKeyRotateDTO } from '@/types/system/security-key';

/**
 * 获取国密 KMS 密钥版本列表
 */
export const listSecurityKeys = (keyAlias?: string) =>
  get<SecurityKeyVersionVO[]>('/system/security/keys', keyAlias ? { keyAlias } : undefined);

/**
 * 获取当前生效的活跃国密密钥
 */
export const getActiveSecurityKey = (keyAlias?: string) =>
  get<SecurityKeyVersionVO>('/system/security/keys/active', keyAlias ? { keyAlias } : undefined);

/**
 * 触发国密 KMS 密钥轮换
 */
export const rotateSecurityKey = (data?: SecurityKeyRotateDTO) =>
  post<SecurityKeyVersionVO>('/system/security/keys/rotate', data || {});
