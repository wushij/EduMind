import { get, post } from '@/core/http/request'
import { LONG_TASK_TIMEOUT } from '@/config'
import type {
  KnowledgeRagDashboardVO,
  KnowledgeRagPurgeResultVO,
  KnowledgeRagSyncResultVO
} from '@/types/knowledge/rag-dashboard'

/** 同步即触发解析/切片/向量化，属于长耗时任务型接口 */
const SYNC_TASK_CONFIG = { timeout: LONG_TASK_TIMEOUT }

export function getKnowledgeRagDashboardStats() {
  return get<KnowledgeRagDashboardVO>('/admin/knowledge/rag-dashboard/stats')
}

export function syncAllKnowledgeRag() {
  return post<KnowledgeRagSyncResultVO>(
    '/admin/knowledge/rag-dashboard/sync-all',
    undefined,
    SYNC_TASK_CONFIG
  )
}

export function syncKnowledgeDocument(documentId: number) {
  return post<KnowledgeRagSyncResultVO>(
    `/admin/knowledge/rag-dashboard/sync-document/${documentId}`,
    undefined,
    SYNC_TASK_CONFIG
  )
}

export function purgeOrphanKnowledgeChunks() {
  return post<KnowledgeRagPurgeResultVO>('/admin/knowledge/rag-dashboard/purge-orphans')
}
