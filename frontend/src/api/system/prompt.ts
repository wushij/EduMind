import { get } from '@/core/http/request';

export const getSystemPrompts = () => get<any[]>('/system/prompts');
