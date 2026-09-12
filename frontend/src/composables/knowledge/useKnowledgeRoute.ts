import { computed } from 'vue';
import { useRoute } from 'vue-router';

/**
 * 从知识库详情路由解析 kbId（/knowledge/:id/...）
 */
export function useKnowledgeRoute() {
  const route = useRoute();

  const kbId = computed(() => {
    const raw = route.params.id;
    const value = Array.isArray(raw) ? raw[0] : raw;
    const id = Number(value);
    return Number.isFinite(id) && id > 0 ? id : undefined;
  });

  return { kbId };
}
