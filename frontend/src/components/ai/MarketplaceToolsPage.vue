<template>
  <div class="ai-marketplace-page">
    <AIMarketplaceHero
      v-model:search-keyword="searchKeyword"
      :active-category="activeCategory"
      @search="(val) => handleSearch(val, true)"
      @instant-search="handleInstantSearch"
    />

    <div v-loading="loading" class="tools-grid-section">
      <div v-if="filteredTools.length > 0" class="tools-grid">
        <AIToolCard
          v-for="tool in filteredTools"
          :key="tool.id"
          :tool="tool"
          @open-detail="openDetail"
        />
      </div>

      <div v-else class="empty-box">
        <el-icon class="empty-icon-svg"><component :is="emptyIcon" /></el-icon>
        <h4>{{ emptyTitle }}</h4>
        <p>{{ emptyDescription }}</p>
        <button
          v-if="searchKeyword"
          type="button"
          class="reset-btn"
          @click="clearSearch"
        >
          清空搜索关键词
        </button>
      </div>
    </div>

    <AIToolDetailDrawer
      v-model="drawerVisible"
      :tool="selectedTool"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue';
import { Search } from '@element-plus/icons-vue';
import AIMarketplaceHero from '@/components/ai/AIMarketplaceHero.vue';
import AIToolCard from '@/components/ai/AIToolCard.vue';
import AIToolDetailDrawer from '@/components/ai/AIToolDetailDrawer.vue';
import {
  useMarketplacePage,
  type MarketplaceCategory
} from '@/composables/ai/useMarketplacePage';

const props = defineProps<{
  activeCategory: MarketplaceCategory;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyIcon?: Component;
}>();

const {
  loading,
  searchKeyword,
  drawerVisible,
  selectedTool,
  filteredTools,
  handleSearch,
  handleInstantSearch,
  openDetail,
  clearSearch
} = useMarketplacePage(props.activeCategory);

const emptyIcon = computed(() => props.emptyIcon ?? Search);
const emptyTitle = computed(() => props.emptyTitle ?? '未找到符合条件的 AI 工具');
const emptyDescription = computed(
  () => props.emptyDescription ?? '请尝试搜索「出题」、「批改」、「助教」等关键词'
);
</script>

<style scoped lang="scss">
.ai-marketplace-page {
  width: 100%;

  .tools-grid-section {
    min-height: 400px;

    .tools-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 20px;
    }

    .empty-box {
      padding: 60px 0;
      text-align: center;
      background: #ffffff;
      border-radius: 18px;
      border: 1px solid #ebf1f7;

      .empty-icon-svg {
        font-size: 38px;
        color: #94a3b8;
        display: block;
        margin: 0 auto 12px;
      }

      h4 {
        margin: 0 0 6px;
        font-size: 16px;
        color: #1e293b;
      }

      p {
        margin: 0 0 16px;
        font-size: 13px;
        color: #94a3b8;
      }

      .reset-btn {
        height: 36px;
        padding: 0 20px;
        border-radius: 9999px;
        background: #f1f5f9;
        border: 1px solid #e2e8f0;
        color: #475569;
        font-size: 13px;
        cursor: pointer;

        &:hover {
          color: #1677ff;
          border-color: #cbd5e1;
        }
      }
    }
  }
}
</style>
