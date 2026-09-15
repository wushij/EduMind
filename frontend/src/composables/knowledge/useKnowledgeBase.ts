import { ref } from 'vue';
import {
  getKnowledgeBases,
  getKnowledgeBaseDetail,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase
} from '@/api/knowledge/knowledge-base';
import { KnowledgeBase } from '@/types/knowledge/knowledge-base';

export function useKnowledgeBase() {
  const knowledgeBases = ref<KnowledgeBase[]>([]);
  const currentKnowledgeBase = ref<KnowledgeBase | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchKnowledgeBases(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getKnowledgeBases(params);
      knowledgeBases.value = (res.data || []).map(normalizeKnowledgeBase);
      total.value = knowledgeBases.value.length;
    } catch (err) {
      knowledgeBases.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchKnowledgeBaseDetail(id: number) {
    loading.value = true;
    try {
      const res = await getKnowledgeBaseDetail(id);
      currentKnowledgeBase.value = res.data ? normalizeKnowledgeBase(res.data as Record<string, any>) : null;
    } catch (err) {
      currentKnowledgeBase.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentKnowledgeBase.value;
  }

  async function create(data: Record<string, any>) {
    const res = await createKnowledgeBase(data);
    return res.data;
  }

  async function update(id: number, data: Record<string, any>) {
    await updateKnowledgeBase(id, data);
    await fetchKnowledgeBaseDetail(id);
  }

  async function remove(id: number) {
    await deleteKnowledgeBase(id);
    knowledgeBases.value = knowledgeBases.value.filter(k => k.id !== id);
    total.value = knowledgeBases.value.length;
  }

  return {
    knowledgeBases,
    currentKnowledgeBase,
    loading,
    total,
    fetchKnowledgeBases,
    fetchKnowledgeBaseDetail,
    create,
    update,
    remove
  };
}

function normalizeKnowledgeBase(raw: Record<string, any>): KnowledgeBase {
  return {
    id: Number(raw.id),
    name: raw.name || '',
    description: raw.description || '',
    category: raw.category || 'MAJOR',
    categoryLabel: raw.categoryLabel || '专业核心',
    documentCount: Number(raw.docCount ?? raw.documentCount ?? 0),
    chunkCount: Number(raw.chunkCount ?? 0),
    vectorStatus: raw.vectorStatus || raw.indexStatus || 'PENDING',
    vectorStatusLabel: raw.vectorStatusLabel || '待解析',
    vectorProgress: raw.vectorProgress ?? 0,
    embeddingModel: raw.embeddingModel || '',
    updatedAt: raw.updateTime || raw.updatedAt || '',
    courseId: raw.courseId,
    courseName: raw.courseName
  };
}
