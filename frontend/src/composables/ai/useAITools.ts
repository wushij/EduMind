import { ref } from 'vue';
import { getAITools } from '@/api/ai/tools';
import { AITool } from '@/types/ai/tool';
import { USE_MOCK } from '@/config/mock';
import { MOCK_AI_TOOLS } from '@/mock/ai-tools';
import { mapTool } from '@/utils/ai/map-tool';

export function useAITools() {
  const tools = ref<AITool[]>([]);
  const loading = ref(false);

  async function fetchTools(category?: string, keyword?: string) {
    loading.value = true;
    try {
      const res = await getAITools(category, keyword);
      tools.value = (res.data || []).map(mapTool);
    } catch {
      tools.value = USE_MOCK ? [...MOCK_AI_TOOLS] : [];
    } finally {
      loading.value = false;
    }
  }

  function filterByCategory(category: string) {
    if (category === 'ALL') return tools.value;
    if (category === 'RECOMMENDED') return tools.value.filter(t => t.isRecommended);
    return tools.value.filter(t => t.category === category);
  }

  return { tools, loading, fetchTools, filterByCategory };
}
