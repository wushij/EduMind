import { computed, ref, unref, watch, onMounted, onUnmounted, type MaybeRef, type Ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getDocuments, uploadDocument, deleteDocument, parseDocument, getDocumentDetail } from '@/api/knowledge/document';
import { getChunks, triggerChunk } from '@/api/knowledge/chunk';
import { getVectorStats, triggerReindex } from '@/api/knowledge/embedding';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import { KBDocument } from '@/types/knowledge/document';
import type { DocumentChunk } from '@/types/knowledge/chunk';
import { parseKnowledgeBaseId, setStoredKnowledgeBaseId } from '@/composables/knowledge/useKnowledgeRoute';
import { normalizeKBDocument } from '@/utils/knowledge/document';
import {
  batchUploadMessage,
  emptyBatchUploadResult,
  type BatchUploadResult
} from '@/utils/upload/coalesce-upload-files';

export type { BatchUploadResult };

export function useDocumentUpload(kbIdInput: MaybeRef<number | undefined>) {
  const kbId = computed(() => parseKnowledgeBaseId(unref(kbIdInput)));
  const documents = ref<KBDocument[]>([]);
  const uploading = ref(false);
  const loading = ref(false);

  async function fetchDocuments() {
    const id = kbId.value;
    if (!id) return;
    loading.value = true;
    try {
      const res = await getDocuments(id);
      documents.value = Array.isArray(res?.data)
        ? res.data.map((item) => normalizeKBDocument(item as unknown as Record<string, unknown>))
        : [];
    } catch {
      documents.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function upload(file: File) {
    const id = kbId.value;
    if (!id) throw new Error('未选择知识库');
    uploading.value = true;
    try {
      const res = await uploadDocument(id, file);
      await fetchDocuments();
      return res;
    } finally {
      uploading.value = false;
    }
  }

  async function uploadMany(files: File[]): Promise<BatchUploadResult> {
    const id = kbId.value;
    if (!id || files.length === 0) return emptyBatchUploadResult();
    uploading.value = true;
    const result: BatchUploadResult = { succeeded: 0, failed: 0, failedNames: [] };
    try {
      for (const file of files) {
        try {
          await uploadDocument(id, file);
          result.succeeded++;
        } catch (err) {
          result.failed++;
          result.failedNames.push(file.name);
        }
      }
      await fetchDocuments();
      return result;
    } finally {
      uploading.value = false;
    }
  }

  async function remove(documentId: number) {
    const id = kbId.value;
    if (!id) throw new Error('未选择知识库');
    await deleteDocument(id, documentId);
    await fetchDocuments();
  }

  async function triggerParse(documentId: number) {
    const id = kbId.value;
    if (!id) throw new Error('未选择知识库');
    await parseDocument(id, documentId);
    await fetchDocuments();
  }

  async function fetchDocumentDetail(documentId: number) {
    const res = await getDocumentDetail(documentId);
    return res.data ? normalizeKBDocument(res.data as unknown as Record<string, unknown>) : null;
  }

  async function triggerRechunk(documentId: number) {
    await triggerChunk(documentId);
  }

  return {
    documents,
    uploading,
    loading,
    fetchDocuments,
    upload,
    uploadMany,
    remove,
    triggerParse,
    triggerRechunk,
    fetchDocumentDetail
  };
}

export function buildDocumentInfo(doc: KBDocument | undefined) {
  if (!doc) {
    return { fileName: '', fileSize: '-', fileType: '-', status: 'PENDING' };
  }
  const sizeBytes = doc.fileSize;
  const sizeMb = sizeBytes ? `${(sizeBytes / 1024 / 1024).toFixed(2)} MB` : '-';
  return {
    fileName: doc.fileName || doc.name || '未知文件',
    fileSize: sizeMb,
    fileType: doc.fileType || 'FILE',
    status: doc.parseStatus ?? doc.chunkStatus ?? 'PENDING'
  };
}

export function isPipelineStepDone(
  step: number,
  docStatus: string,
  chunkCount: number,
  indexedChunks: number
): boolean {
  const normStatus = (docStatus || '').toUpperCase();
  if (step === 1) {
    return (
      ['COMPLETED', 'SUCCESS', 'PARSED', 'CHUNKED', 'INDEXED'].includes(normStatus) ||
      chunkCount > 0
    );
  }
  if (step === 2) {
    return chunkCount > 0 || ['CHUNKED', 'INDEXED'].includes(normStatus);
  }
  if (step === 3) {
    return indexedChunks > 0 || normStatus === 'INDEXED';
  }
  return false;
}

export function useDocumentParse(kbId: Ref<number | undefined>) {
  const route = useRoute();
  const router = useRouter();

  const knowledgeBases = ref<KnowledgeBase[]>([]);
  const kbLoading = ref(false);
  const documents = ref<KBDocument[]>([]);
  const selectedDocumentId = ref<number | undefined>();
  const chunks = ref<DocumentChunk[]>([]);
  const loading = ref(false);
  const pipelineRunning = ref(false);
  const indexStatus = ref<{
    status?: string;
    indexedChunks?: number;
    totalChunks?: number;
    embeddingMocked?: boolean;
    embeddingModel?: string;
  }>({});
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const docInfo = computed(() => {
    const doc = documents.value.find((d) => Number(d.id) === Number(selectedDocumentId.value));
    return buildDocumentInfo(doc);
  });

  async function loadKnowledgeBases() {
    kbLoading.value = true;
    try {
      const res = await getKnowledgeBases();
      knowledgeBases.value = Array.isArray(res?.data) ? res.data : [];
    } catch {
      knowledgeBases.value = [];
    } finally {
      kbLoading.value = false;
    }
  }

  function handleKbChange(newKbId: number) {
    if (!newKbId || newKbId === kbId.value) return;
    setStoredKnowledgeBaseId(newKbId);
    router.push({ path: `/knowledge/${newKbId}/parse` });
  }

  async function loadDocuments() {
    if (!kbId.value) return;
    try {
      const res = await getDocuments(kbId.value);
      documents.value = Array.isArray(res?.data)
        ? res.data.map((item) => normalizeKBDocument(item as unknown as Record<string, unknown>))
        : [];
      const queryDocId = Number(route.query.documentId);
      if (queryDocId && documents.value.some((d) => Number(d.id) === queryDocId)) {
        selectedDocumentId.value = queryDocId;
      } else if (documents.value.length > 0) {
        if (!selectedDocumentId.value || !documents.value.some((d) => Number(d.id) === Number(selectedDocumentId.value))) {
          selectedDocumentId.value = Number(documents.value[0].id);
        }
      } else {
        selectedDocumentId.value = undefined;
      }
    } catch {
      ElMessage.error('加载文档列表失败');
    }
  }

  async function loadChunks() {
    if (!selectedDocumentId.value) {
      chunks.value = [];
      return;
    }
    loading.value = true;
    try {
      chunks.value = await getChunks(selectedDocumentId.value, { page: 1, pageSize: 100 });
    } catch {
      // 静默清空会让「切片其实已完成」看起来像「什么都没生成」，必须显式提示
      chunks.value = [];
      ElMessage.error('加载切片列表失败，请稍后重试或重新选择文档');
      ElMessage.error('加载切片失败');
    } finally {
      loading.value = false;
    }
  }

  async function loadIndexStatus() {
    if (!kbId.value) return;
    try {
      const stats = await getVectorStats(kbId.value);
      indexStatus.value = {
        status: stats.connectionStatus === 'ONLINE' ? 'INDEXED' : 'INDEXING',
        indexedChunks: stats.totalVectors,
        totalChunks: stats.expectedVectors,
        embeddingMocked: stats.embeddingMocked,
        embeddingModel: stats.embeddingModel
      };
    } catch {
      indexStatus.value = {};
    }
  }

  async function handleDocumentChange() {
    await loadChunks();
    await loadIndexStatus();
  }

  async function handleRunPipeline() {
    if (!kbId.value || !selectedDocumentId.value) {
      ElMessage.warning('请先选择文档');
      return;
    }
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
    pipelineRunning.value = true;
    try {
      await parseDocument(kbId.value, selectedDocumentId.value);
      await triggerChunk(selectedDocumentId.value);
      ElMessage.success('文档流水线已启动：解析 → 切片 → 向量化');

      await loadDocuments();
      await loadChunks();
      await loadIndexStatus();

      // 未接入真实向量模型时明确告知：这类向量没有语义，不要把"已向量化"当成检索可用
      if (indexStatus.value.embeddingMocked) {
        ElMessage.warning({
          message: '当前未接入真实向量模型，本次向量为 Mock 伪向量，检索结果不可信；请先配置 Embedding 模型后重新执行',
          duration: 6000
        });
      }

      // 异步轻量轮询追踪进度（最多轮询 6 次，每 1.2 秒一次）
      let count = 0;
      pollTimer = setInterval(async () => {
        count++;
        await loadDocuments();
        await loadChunks();
        await loadIndexStatus();
        if ((chunks.value.length > 0 && (indexStatus.value.indexedChunks ?? 0) > 0) || count >= 6) {
          if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
          }
        }
      }, 1200);
    } catch (err) {
      // 请求失败（尤其是超时）不代表流水线没跑：解析/切片/向量化可能仍在服务端执行，
      // 这里主动回读一次真实状态，避免用户对着空白面板以为「什么都没发生」。
      await Promise.allSettled([loadDocuments(), loadChunks(), loadIndexStatus()]);
      ElMessage.error(err instanceof Error ? err.message : '流水线执行失败');
    } finally {
      pipelineRunning.value = false;
    }
  }

  watch(kbId, () => {
    loadDocuments().then(() => {
      loadChunks();
      loadIndexStatus();
    });
  });

  onMounted(async () => {
    await loadKnowledgeBases();
    await loadDocuments();
    await loadChunks();
    await loadIndexStatus();
  });

  onUnmounted(() => {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  });

  return {
    knowledgeBases,
    kbLoading,
    documents,
    selectedDocumentId,
    chunks,
    loading,
    pipelineRunning,
    indexStatus,
    docInfo,
    loadKnowledgeBases,
    handleKbChange,
    loadDocuments,
    loadChunks,
    loadIndexStatus,
    handleDocumentChange,
    handleRunPipeline,
    isPipelineStepDone
  };
}
