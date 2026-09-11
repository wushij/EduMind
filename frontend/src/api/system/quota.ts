import { get } from '@/core/http/request';

export const getQuotas = () => get<any[]>('/system/quotas');
