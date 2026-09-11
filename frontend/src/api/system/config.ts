import { get } from '@/core/http/request';

export const getSystemConfigs = () => get<any>('/system/configs');
