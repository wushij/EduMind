import { computed, ref, unref, type MaybeRef } from 'vue';
import { getDocuments, uploadDocument, deleteDocument, parseDocument, getDocumentDetail } from '@/api/knowledge/document';
import { KBDocument } from '@/types/knowledge/document';
import { parseKnowledgeBaseId } from '@/composables/knowledge/useKnowledgeRoute';

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
      documents.value = res.data || [];
    } catch {
      documents.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function upload(file: File) {
    const id = kbId.value;
    if (!id) return;
    uploading.value = true;
    try {
      const res = await uploadDocument(id, file);
      await fetchDocuments();
      return res.data;
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
    return res.data;
  }

  return {
    documents,
    uploading,
    loading,
    fetchDocuments,
    upload,
    remove,
    triggerParse,
    fetchDocumentDetail
  };
}
