import { get, post, put, del } from '@/core/http/request';
import type { HttpRequestConfig } from '@/core/http/types';
import { KnowledgeBase } from '@/types/knowledge/knowledge-base';

export const getKnowledgeBases = (params?: Record<string, any>, config?: HttpRequestConfig) =>
  get<KnowledgeBase[]>('/knowledge-bases', params, config);

export const getKnowledgeBaseDetail = (id: number, config?: HttpRequestConfig) =>
  get<KnowledgeBase>(`/knowledge-bases/${id}`, undefined, config);

export const createKnowledgeBase = (data: Record<string, any>) => post<number>('/knowledge-bases', data);

export const updateKnowledgeBase = (id: number, data: Record<string, any>) =>
  put<void>(`/knowledge-bases/${id}`, data);

export const deleteKnowledgeBase = (id: number) => del<void>(`/knowledge-bases/${id}`);
