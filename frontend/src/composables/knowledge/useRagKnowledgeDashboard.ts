import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKnowledgeRagDashboardStats,
  purgeOrphanKnowledgeChunks,
  syncAllKnowledgeRag,
  syncKnowledgeDocument
} from '@/api/knowledge/rag-dashboard'
import type { KnowledgeRagDashboardVO } from '@/types/knowledge/rag-dashboard'

const emptyStats = (): KnowledgeRagDashboardVO => ({
  totalDocuments: 0,
  indexedDocuments: 0,
  lessonDocuments: 0,
  uploadDocuments: 0,
  totalChunks: 0,
  lessonChunks: 0,
  uploadChunks: 0,
  indexedChunks: 0,
  failedChunks: 0,
  normalChunks: 0,
  orphanChunkEstimate: 0,
  totalSessions: 0,
  hybridEnabled: true
})

export function useRagKnowledgeDashboard() {
  const loading = ref(false)
  const syncingAll = ref(false)
  const purging = ref(false)
  const syncingDoc = ref(false)
  const stats = ref<KnowledgeRagDashboardVO>(emptyStats())
  const targetDocumentId = ref('')
  const syncBanner = ref<string | null>(null)

  const healthRate = computed(() => {
    if (stats.value.indexHealthPercent != null) {
      return stats.value.indexHealthPercent
    }
    const indexed = stats.value.indexedChunks || 0
    if (!indexed) {
      return (stats.value.totalChunks || 0) <= 0 ? 100 : 0
    }
    const normal = stats.value.normalChunks || 0
    return Math.min(100, Math.round((normal / indexed) * 1000) / 10)
  })

  async function fetchStats(silent = false) {
    if (!silent) loading.value = true
    try {
      const res = await getKnowledgeRagDashboardStats()
      if (res?.data) stats.value = { ...emptyStats(), ...res.data }
    } catch (e: unknown) {
      if (!silent) {
        ElMessage.error(e instanceof Error ? e.message : '获取 RAG 大盘失败')
      }
    } finally {
      if (!silent) loading.value = false
    }
  }

  async function handleSyncAll() {
    try {
      await ElMessageBox.confirm(
        '将对全部课程知识库执行 FULL 向量重建，并同步已发布课节讲义索引。任务在后台执行，请稍后刷新大盘。',
        '全量切片同步与重建',
        { type: 'warning', confirmButtonText: '开始同步' }
      )
    } catch {
      return
    }
    syncingAll.value = true
    try {
      const res = await syncAllKnowledgeRag()
      const body = res?.data
      syncBanner.value = `已触发 ${body?.knowledgeBasesTriggered ?? 0} 个知识库 FULL 索引，课节重索引 ${body?.lessonsReindexed ?? 0} 个`
      ElMessage.success('全量同步任务已启动')
      await fetchStats(true)
    } catch (e: unknown) {
      ElMessage.error(e instanceof Error ? e.message : '全量同步失败')
    } finally {
      syncingAll.value = false
    }
  }

  async function handlePurgeOrphans() {
    purging.value = true
    try {
      const res = await purgeOrphanKnowledgeChunks()
      ElMessage.success(`已清理无效切片 ${res?.data?.purgedChunks ?? 0} 条`)
      await fetchStats(true)
    } catch (e: unknown) {
      ElMessage.error(e instanceof Error ? e.message : '清理失败')
    } finally {
      purging.value = false
    }
  }

  async function handleSyncDocument() {
    const id = Number(targetDocumentId.value)
    if (!id || id <= 0) {
      ElMessage.warning('请输入有效的文档 ID')
      return
    }
    syncingDoc.value = true
    try {
      await syncKnowledgeDocument(id)
      ElMessage.success(`文档 #${id} 已触发增量重建`)
      targetDocumentId.value = ''
      await fetchStats(true)
    } catch (e: unknown) {
      ElMessage.error(e instanceof Error ? e.message : '单文档同步失败')
    } finally {
      syncingDoc.value = false
    }
  }

  return {
    loading,
    syncingAll,
    purging,
    syncingDoc,
    stats,
    targetDocumentId,
    syncBanner,
    healthRate,
    fetchStats,
    handleSyncAll,
    handlePurgeOrphans,
    handleSyncDocument
  }
}
