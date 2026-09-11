import { get, post, put, del } from '@/core/http/request';
import { KnowledgeBase } from '@/types/knowledge/knowledge-base';

export const getKnowledgeBases = (params?: Record<string, any>) =>
  get<KnowledgeBase[]>('/knowledge-bases', params);

export const getKnowledgeBaseDetail = (id: number) => get<KnowledgeBase>(`/knowledge-bases/${id}`);

export const createKnowledgeBase = (data: Record<string, any>) => post<number>('/knowledge-bases', data);

export const updateKnowledgeBase = (id: number, data: Record<string, any>) =>
  put<void>(`/knowledge-bases/${id}`, data);

export const deleteKnowledgeBase = (id: number) => del<void>(`/knowledge-bases/${id}`);
