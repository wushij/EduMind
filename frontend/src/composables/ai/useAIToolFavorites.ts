import { ref, computed } from 'vue';
import type { AITool } from '@/types/ai/tool';

const STORAGE_KEY = 'edumind:ai-tool-favorites';

const favoriteIds = ref<string[]>(loadFavoriteIds());

function loadFavoriteIds(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((id) => typeof id === 'string') : [];
  } catch {
    return [];
  }
}

function persistFavoriteIds(ids: string[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
}

export function useAIToolFavorites() {
  function isFavorite(toolId: string) {
    return favoriteIds.value.includes(toolId);
  }

  function toggleFavorite(toolId: string) {
    const idx = favoriteIds.value.indexOf(toolId);
    if (idx > -1) {
      favoriteIds.value = favoriteIds.value.filter((id) => id !== toolId);
    } else {
      favoriteIds.value = [...favoriteIds.value, toolId];
    }
    persistFavoriteIds(favoriteIds.value);
  }

  function syncFavoriteState(tools: AITool[]) {
    tools.forEach((tool) => {
      tool.isFavorite = isFavorite(tool.id);
    });
  }

  return {
    favoriteIds: computed(() => favoriteIds.value),
    isFavorite,
    toggleFavorite,
    syncFavoriteState
  };
}
