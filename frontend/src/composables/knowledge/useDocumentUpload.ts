import { ref } from 'vue';
import { getDocuments, uploadDocument, deleteDocument, parseDocument, getDocumentDetail } from '@/api/knowledge/document';
import { KBDocument } from '@/types/knowledge/document';

export function useDocumentUpload(kbId: number) {
  const documents = ref<KBDocument[]>([]);
  const uploading = ref(false);
  const loading = ref(false);

  async function fetchDocuments() {
    loading.value = true;
    try {
      const res = await getDocuments(kbId);
      documents.value = res.data || [];
    } catch {
      documents.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function upload(file: File) {
    uploading.value = true;
    try {
      const res = await uploadDocument(kbId, file);
      await fetchDocuments();
      return res.data;
    } finally {
      uploading.value = false;
    }
  }

  async function remove(documentId: number) {
    await deleteDocument(kbId, documentId);
    documents.value = documents.value.filter(d => d.id !== documentId);
  }

  async function triggerParse(documentId: number) {
    await parseDocument(kbId, documentId);
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
