import { get, put, post } from '@/core/http/request';
import type { MailConfigVO, MailConfigDTO } from '@/types/system';

export const getSystemConfigs = () => get<any>('/system/configs');

export const getMailConfig = () => get<MailConfigVO>('/system/configs/mail');

export const updateMailConfig = (data: MailConfigDTO) => put<void>('/system/configs/mail', data);

export const testMailConfig = (toEmail: string) => post<void>('/system/configs/mail/test', { toEmail });

export const getBaseInfo = () => get<Record<string, any>>('/system/configs/base');
