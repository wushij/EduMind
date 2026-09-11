import { post } from '@/core/http/request';
import { RetrievalResult } from '@/types/knowledge/retrieval';

export const testRetrieval = (baseId: number, query: string) => post<RetrievalResult>('/knowledge/retrieval/test', { baseId, query });
