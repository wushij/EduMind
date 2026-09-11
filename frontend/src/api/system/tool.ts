import { get } from '@/core/http/request';

export const getSystemTools = () => get<any[]>('/system/tools');
