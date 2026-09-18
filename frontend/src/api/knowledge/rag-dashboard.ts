import { get, post } from '@/core/http/request'
import type {
  KnowledgeRagDashboardVO,
  KnowledgeRagPurgeResultVO,
  KnowledgeRagSyncResultVO
} from '@/types/knowledge/rag-dashboard'

export function getKnowledgeRagDashboardStats() {
  return get<KnowledgeRagDashboardVO>('/admin/knowledge/rag-dashboard/stats')
}

export function syncAllKnowledgeRag() {
  return post<KnowledgeRagSyncResultVO>('/admin/knowledge/rag-dashboard/sync-all')
}

export function syncKnowledgeDocument(documentId: number) {
  return post<KnowledgeRagSyncResultVO>(`/admin/knowledge/rag-dashboard/sync-document/${documentId}`)
}

export function purgeOrphanKnowledgeChunks() {
  return post<KnowledgeRagPurgeResultVO>('/admin/knowledge/rag-dashboard/purge-orphans')
}
