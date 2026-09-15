<template>
  <div class="knowledge-base-page-container">
    <KnowledgeBaseListBanner
      :kb-banner-img="kbBannerImg"
      :knowledge-base-count="knowledgeBases.length"
      :total-docs="totalDocs"
      :total-chunks="totalChunks"
      @create="showCreateDialog = true"
    />

    <div class="kb-tabs-filter-bar">
      <div class="pill-tabs-track">
        <button
          v-for="tab in categoryTabs"
          :key="tab.value"
          type="button"
          class="pill-tab-item"
          :class="{ active: selectedCategory === tab.value }"
          @click="selectedCategory = tab.value"
        >
          <el-icon v-if="tab.icon" class="tab-icon"><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <span class="tab-count-pill">{{ getCategoryCount(tab.value) }}</span>
        </button>
      </div>

      <div class="filter-right-actions">
        <input
          v-model="searchKeyword"
          type="text"
          class="kb-inline-search"
          placeholder="搜索知识库、文档或课程..."
        />
        <span class="total-hint">共展示 {{ filteredList.length }} 个知识库</span>
      </div>
    </div>

    <div v-if="filteredList.length > 0" class="knowledge-cards-grid">
      <KnowledgeBaseCard
        v-for="item in filteredList"
        :key="item.id"
        :item="item"
        @open="handleOpenDetail"
        @upload="handleUploadDoc"
        @delete="handleDeleteKb"
      />
    </div>

    <div v-else class="empty-kb-panel">
      <el-icon class="empty-icon"><Search /></el-icon>
      <h3 class="empty-title">未找到匹配的知识库</h3>
      <p class="empty-hint">建议调整上方搜索关键字或分类筛选条件，也可以点击上方按钮快速创建。</p>
      <button
        type="button"
        class="capsule-reset-btn"
        @click="resetFilters"
      >
        <span>重置筛选</span>
      </button>
    </div>

    <KnowledgeBaseCreateDialog
      v-model:visible="showCreateDialog"
      :create-form="createForm"
      :create-rules="createRules"
      :category-options="categoryTabs.filter((t) => t.value !== 'ALL')"
      @confirm="handleConfirmCreate"
    />
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue';
import KnowledgeBaseCard from '@/components/knowledge/KnowledgeBaseCard.vue';
import KnowledgeBaseListBanner from '@/components/knowledge/KnowledgeBaseListBanner.vue';
import KnowledgeBaseCreateDialog from '@/components/knowledge/KnowledgeBaseCreateDialog.vue';
import { useKnowledgeBaseList } from '@/composables/knowledge/useKnowledgeBaseList';

const {
  kbBannerImg,
  knowledgeBases,
  selectedCategory,
  searchKeyword,
  showCreateDialog,
  categoryTabs,
  totalDocs,
  totalChunks,
  filteredList,
  createForm,
  createRules,
  getCategoryCount,
  resetFilters,
  handleOpenDetail,
  handleUploadDoc,
  handleDeleteKb,
  handleConfirmCreate
} = useKnowledgeBaseList();
</script>

<style scoped lang="scss">
.knowledge-base-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .kb-tabs-filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #FFFFFF;
    border-radius: 18px;
    padding: 8px 14px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);

    .pill-tabs-track {
      display: flex;
      align-items: center;
      gap: 8px;
      overflow-x: auto;

      .pill-tab-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 16px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        color: #64748B;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;

        .tab-icon {
          font-size: 14px;
        }

        .tab-count-pill {
          padding: 1px 8px;
          border-radius: 9999px;
          background: #E2E8F0;
          color: #475569;
          font-size: 11px;
          font-weight: 600;
        }

        &:hover {
          color: #1677FF;
          background: #EFF6FF;
        }

        &.active {
          background: #1677FF;
          border-color: #1677FF;
          color: #FFFFFF;
          font-weight: 600;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

          .tab-count-pill {
            background: rgba(255, 255, 255, 0.25);
            color: #FFFFFF;
          }
        }
      }
    }

    .filter-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .kb-inline-search {
        width: 220px;
        height: 34px;
        padding: 0 14px;
        border-radius: 9999px;
        border: 1px solid #E2E8F0;
        background: #F8FAFC;
        font-size: 13px;
        color: #334155;
        outline: none;

        &:focus {
          border-color: #1677FF;
          background: #FFFFFF;
        }
      }

      .total-hint {
        font-size: 12.5px;
        color: #94A3B8;
      }
    }
  }

  .knowledge-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .empty-kb-panel {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px dashed #CBD5E1;
    padding: 60px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-icon {
      font-size: 44px;
      color: #94A3B8;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .empty-hint {
      font-size: 13px;
      color: #94A3B8;
      margin: 0 0 18px 0;
    }

    .capsule-reset-btn {
      height: 38px;
      padding: 0 24px;
      border-radius: 9999px;
      background: #1677FF;
      color: #FFFFFF;
      font-size: 13.5px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #4096FF;
      }
    }
  }
}

@media (max-width: 1280px) {
  .knowledge-cards-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 768px) {
  .knowledge-cards-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
