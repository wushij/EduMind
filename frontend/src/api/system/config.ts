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

