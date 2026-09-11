import { get } from '@/core/http/request';

export const getAuditLogs = () => get<any[]>('/system/audit-logs');
