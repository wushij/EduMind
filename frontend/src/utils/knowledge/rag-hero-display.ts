import type { KnowledgeRagDashboardVO } from '@/types/knowledge/rag-dashboard'

export function resolveRagIndexHealthPercent(stats: KnowledgeRagDashboardVO): number {
  if (stats.indexHealthPercent != null && !Number.isNaN(stats.indexHealthPercent)) {
    return stats.indexHealthPercent
  }
  const indexed = stats.indexedChunks || 0
  const total = stats.totalChunks || 0
  if (indexed > 0) {
    const normal = stats.normalChunks ?? Math.max(0, indexed - (stats.failedChunks || 0))
    return Math.min(100, Math.round((normal / indexed) * 1000) / 10)
  }
  return total <= 0 ? 100 : 0
}

export function resolveRagIndexStatusText(stats: KnowledgeRagDashboardVO): string {
  if (stats.latestIndexStatus === 'INDEXING') {
    return '构建中'
  }
  if ((stats.failedChunks ?? 0) > 0) {
    return '部分异常'
  }
  const total = stats.totalChunks ?? 0
  const indexed = stats.indexedChunks ?? 0
  if (total > 0 && indexed < total) {
    return '同步中'
  }
  return '正常运行'
}

export function resolveRagChunkIndexPercent(stats: KnowledgeRagDashboardVO): number {
  const total = stats.totalChunks ?? 0
  const indexed = stats.indexedChunks ?? 0
  if (total <= 0) {
    return 100
  }
  return Math.min(100, Math.round((indexed / total) * 100))
}
