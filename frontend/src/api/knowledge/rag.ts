import { post } from '@/core/http/request';

export const debugRagPipeline = (data: any) => post<any>('/knowledge/rag/debug', data);
