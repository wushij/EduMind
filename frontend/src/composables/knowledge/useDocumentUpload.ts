import { computed, ref, unref, watch, onMounted, type MaybeRef, type Ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getDocuments, uploadDocument, deleteDocument, parseDocument, getDocumentDetail } from '@/api/knowledge/document';
import { getChunks, triggerChunk } from '@/api/knowledge/chunk';
import { getVectorStats, triggerReindex } from '@/api/knowledge/embedding';
import { KBDocument } from '@/types/knowledge/document';
import type { DocumentChunk } from '@/types/knowledge/chunk';
import { parseKnowledgeBaseId } from '@/composables/knowledge/useKnowledgeRoute';
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
      const list = Array.isArray(res.data) ? res.data : [];
      documents.value = list.map((item) => normalizeKBDocument(item as unknown as Record<string, unknown>));
    } catch {
      documents.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function upload(file: File) {
    const result = await uploadMany([file]);
    if (result.failed > 0) {
      throw new Error(result.failedNames[0] || '文档上传失败');
    }
    return result.lastUploaded;
  }

  async function uploadMany(files: File[]): Promise<BatchUploadResult & { lastUploaded?: unknown }> {
    const id = kbId.value;
    if (!id || files.length === 0) {
      return { ...emptyBatchUploadResult(), lastUploaded: undefined };
    }

    uploading.value = true;
    const result: BatchUploadResult & { lastUploaded?: unknown } = {
      ...emptyBatchUploadResult(),
      lastUploaded: undefined
    };

    try {
      for (const file of files) {
        try {
          const res = await uploadDocument(id, file);
          result.succeeded += 1;
          result.lastUploaded = res.data;
        } catch {
          result.failed += 1;
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
    if (!id) return;
    await deleteDocument(id, documentId);
    documents.value = documents.value.filter(d => d.id !== documentId);
  }

  async function triggerParse(documentId: number) {
    const id = kbId.value;
    if (!id) return;
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
    fileName: doc.name ?? doc.fileName,
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
  if (step === 1) return docStatus === 'COMPLETED';
  if (step === 2) return chunkCount > 0;
  if (step === 3) return indexedChunks > 0;
  return false;
}

export function useDocumentParse(kbId: Ref<number | undefined>) {
  const route = useRoute();
  const documents = ref<KBDocument[]>([]);
  const selectedDocumentId = ref<number | undefined>();
  const chunks = ref<DocumentChunk[]>([]);
  const loading = ref(false);
  const pipelineRunning = ref(false);
  const indexStatus = ref<{ status?: string; indexedChunks?: number; totalChunks?: number }>({});

  const docInfo = computed(() => {
    const doc = documents.value.find((d) => d.id === selectedDocumentId.value);
    return buildDocumentInfo(doc);
  });

  async function loadDocuments() {
    if (!kbId.value) return;
    try {
      const res = await getDocuments(kbId.value);
      documents.value = Array.isArray(res?.data) ? res.data : [];
      const queryDocId = Number(route.query.documentId);
      if (queryDocId && documents.value.some((d) => d.id === queryDocId)) {
        selectedDocumentId.value = queryDocId;
      } else if (documents.value.length > 0) {
        selectedDocumentId.value = documents.value[0].id;
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
      chunks.value = [];
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
        totalChunks: stats.expectedVectors
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
    pipelineRunning.value = true;
    try {
      await parseDocument(kbId.value, selectedDocumentId.value);
      await triggerChunk(selectedDocumentId.value);
      await triggerReindex(kbId.value, 'INCREMENTAL');
      ElMessage.success('文档流水线已启动：解析 → 切片 → 向量化');
      await loadDocuments();
      await loadChunks();
      await loadIndexStatus();
    } catch (err) {
      ElMessage.error(err instanceof Error ? err.message : '流水线执行失败');
    } finally {
      pipelineRunning.value = false;
    }
  }

  watch(kbId, () => {
    loadDocuments();
    loadIndexStatus();
  });

  onMounted(async () => {
    await loadDocuments();
    await loadChunks();
    await loadIndexStatus();
  });

  return {
    documents,
    selectedDocumentId,
    chunks,
    loading,
    pipelineRunning,
    indexStatus,
    docInfo,
    loadDocuments,
    loadChunks,
    loadIndexStatus,
    handleDocumentChange,
    handleRunPipeline,
    isPipelineStepDone
  };
}
