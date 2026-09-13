import { computed } from 'vue';
import { useRoute } from 'vue-router';

export const LAST_KNOWLEDGE_ID_KEY = 'edumind_last_knowledge_id';

/** 从路由参数或路径片段解析知识库 ID（仅接受正整数） */
export function parseKnowledgeBaseId(raw: unknown): number | undefined {
  const value = Array.isArray(raw) ? raw[0] : raw;
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

/** 从 /knowledge/:id/... 路径中提取知识库 ID */
export function parseKnowledgeBaseIdFromPath(path: string): number | undefined {
  const match = path.match(/^\/knowledge\/(\d+)(?:\/|$)/);
  return match ? parseKnowledgeBaseId(match[1]) : undefined;
}

export function getStoredKnowledgeBaseId(): number {
  const stored = localStorage.getItem(LAST_KNOWLEDGE_ID_KEY);
  return parseKnowledgeBaseId(stored) ?? 1;
}

/**
 * 从知识库详情路由解析 kbId（/knowledge/:id/...）
 */
export function useKnowledgeRoute() {
  const route = useRoute();

  const kbId = computed(() => parseKnowledgeBaseId(route.params.id));

  return { kbId };
}
