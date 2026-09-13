import { get, put, post } from '@/core/http/request';
import type {
  MailConfigVO,
  MailConfigDTO,
  StorageConfigVO,
  StorageConfigDTO,
  SecurityConfigVO,
  SecurityConfigDTO
} from '@/types/system';

export const getSystemConfigs = () => get<any>('/system/configs');

export const getMailConfig = () => get<MailConfigVO>('/system/configs/mail');

export const updateMailConfig = (data: MailConfigDTO) => put<void>('/system/configs/mail', data);

export const testMailConfig = (toEmail: string) => post<void>('/system/configs/mail/test', { toEmail });

export const getStorageConfig = () => get<StorageConfigVO>('/system/configs/storage');

export const updateStorageConfig = (data: StorageConfigDTO) => put<void>('/system/configs/storage', data);

export const testStorageConfig = (data: StorageConfigDTO) => post<void>('/system/configs/storage/test', data);

export const getSecurityConfig = () => get<SecurityConfigVO>('/system/configs/security');

export const updateSecurityConfig = (data: SecurityConfigDTO) => put<void>('/system/configs/security', data);

export const getBaseInfo = () => get<Record<string, any>>('/system/configs/base');

// 12 大通用配置分组 API
export const getConfigGroups = () => get<any[]>('/system/configs/groups');

export const getConfigGroup = (groupCode: string) =>
  get<{ groupCode: string; groupName: string; configValue: string; remark?: string }>(
    `/system/configs/groups/${groupCode}`
  );

export const updateConfigGroup = (groupCode: string, configValue: string) =>
  put<void>(`/system/configs/groups/${groupCode}`, { configValue });

export const testSms = (data: { phone: string; templateCode?: string }) =>
  post<boolean>('/system/configs/test-sms', data);

export const getRecentSmsLogs = (limit = 5) =>
  get<any[]>('/system/configs/sms-logs/recent', { limit });

export const getSmsLogs = (params: { page?: number; size?: number; phone?: string; status?: number }) =>
  get<{ total: number; list: any[] }>('/system/configs/sms-logs', params);

export const testPayment = (data: { type: 'wechat' | 'alipay' }) =>
  post<Record<string, string>>('/system/configs/test-payment', data);

export const getRecentEmailLogs = (limit = 5) =>
  get<any[]>('/system/configs/email-logs/recent', { limit });

export const getEmailLogs = (params: { page?: number; size?: number; email?: string; status?: number }) =>
  get<{ total: number; list: any[] }>('/system/configs/email-logs', params);

export const getRoleOptions = () =>
  get<any[]>('/system/configs/role-options');

export const getUserOptions = () =>
  get<any[]>('/system/configs/user-options');

