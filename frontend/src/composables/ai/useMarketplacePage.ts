import { ref, computed, onMounted, watch } from 'vue';
import type { AITool } from '@/types/ai/tool';
import { useAITools } from '@/composables/ai/useAITools';
import { useAIToolFavorites } from '@/composables/ai/useAIToolFavorites';
import { useAuthStore } from '@/stores/auth/auth';
import { filterToolsByRole } from '@/utils/ai/tool-role-access';

export type MarketplaceCategory = 'ALL' | 'TEACHER' | 'STUDENT' | 'RECOMMENDED' | 'MY_TOOLS';

export function useMarketplacePage(activeCategory: MarketplaceCategory) {
  const { tools, loading, fetchTools } = useAITools();
  const { favoriteIds, isFavorite, syncFavoriteState } = useAIToolFavorites();
  const authStore = useAuthStore();
  const currentRole = computed(() => authStore.currentRole);

  const searchKeyword = ref('');
  const drawerVisible = ref(false);
  const selectedTool = ref<AITool | null>(null);

  function applyCategoryFilter(list: AITool[]) {
    switch (activeCategory) {
      case 'TEACHER':
        return list.filter((t) => t.category === 'TEACHER');
      case 'STUDENT':
        return list.filter((t) => t.category === 'STUDENT');
      case 'RECOMMENDED':
        return list.filter((t) => t.isRecommended);
      case 'MY_TOOLS':
        return list.filter((t) => favoriteIds.value.includes(t.id));
      default:
        return list;
    }
  }

  function applyKeywordFilter(list: AITool[]) {
    const kw = searchKeyword.value.trim().toLowerCase();
    if (!kw) return list;
    return list.filter(
      (t) =>
        t.name.toLowerCase().includes(kw) ||
        t.description.toLowerCase().includes(kw) ||
        t.tags.some((tag) => tag.toLowerCase().includes(kw))
    );
  }

  const filteredTools = computed(() => {
    const withFavorites = tools.value.map((tool) => ({
      ...tool,
      isFavorite: isFavorite(tool.id)
    }));
    const roleScopedTools = filterToolsByRole(withFavorites, currentRole.value);
    return applyKeywordFilter(applyCategoryFilter(roleScopedTools));
  });

  function loadTools(useBackendKeyword = false) {
    const apiCategory = activeCategory === 'MY_TOOLS' ? 'ALL' : activeCategory;
    const keyword = useBackendKeyword ? searchKeyword.value.trim() || undefined : undefined;
    return fetchTools(apiCategory, keyword).then(() => {
      syncFavoriteState(tools.value);
    });
  }

  function handleSearch(val: string, fromBackend = true) {
    searchKeyword.value = val;
    if (fromBackend) {
      loadTools(true);
    }
  }

  function handleInstantSearch(val: string) {
    searchKeyword.value = val;
  }

  function openDetail(tool: AITool) {
    selectedTool.value = tool;
    drawerVisible.value = true;
  }

  function clearSearch() {
    searchKeyword.value = '';
    loadTools(true);
  }

  onMounted(() => loadTools(false));

  watch(favoriteIds, () => {
    syncFavoriteState(tools.value);
  });

  return {
    tools,
    loading,
    searchKeyword,
    drawerVisible,
    selectedTool,
    filteredTools,
    loadTools,
    handleSearch,
    handleInstantSearch,
    openDetail,
    clearSearch
  };
}
