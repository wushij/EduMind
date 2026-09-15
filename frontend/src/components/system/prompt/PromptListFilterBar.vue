<template>
  <div class="filter-bar-card">
    <div class="category-tabs">
      <button
        v-for="cat in categoryOptions"
        :key="cat.key"
        class="cat-tab-btn"
        :class="{ active: selectedCategory === cat.key }"
        @click="$emit('change-category', cat.key)"
      >
        <span class="tab-label">{{ cat.label }}</span>
        <span class="tab-badge" :class="cat.key">{{ getCategoryCount(cat.key) }}</span>
      </button>
    </div>

    <div class="filter-right-tools">
      <el-select
        :model-value="selectedStatus"
        placeholder="全部状态"
        style="width: 110px"
        clearable
        @update:model-value="$emit('update:selectedStatus', $event); $emit('filter')"
      >
        <el-option label="全部状态" value="" />
        <el-option label="运行中 (Online)" value="PUBLISHED" />
        <el-option label="草稿 (Draft)" value="DRAFT" />
      </el-select>

      <el-input
        :model-value="searchKeyword"
        placeholder="按名称、编码或变量检索..."
        style="width: 220px"
        clearable
        :prefix-icon="Search"
        @update:model-value="$emit('update:searchKeyword', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue';
import { categoryOptions } from '@/composables/system/usePromptList';

defineProps<{
  selectedCategory: string;
  selectedStatus: string;
  searchKeyword: string;
  getCategoryCount: (catKey: string) => number;
}>();

defineEmits<{
  'change-category': [catKey: string];
  'update:selectedStatus': [value: string];
  'update:searchKeyword': [value: string];
  filter: [];
}>();
</script>

<style scoped lang="scss">
.filter-bar-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 8px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap !important;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

  .category-tabs {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-wrap: nowrap !important;
    flex-shrink: 1;
    min-width: 0;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar {
      display: none;
    }

    .cat-tab-btn {
      background: transparent;
      border: none;
      outline: none;
      cursor: pointer;
      padding: 5px 11px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      color: #475569;
      display: flex;
      align-items: center;
      gap: 6px;
      white-space: nowrap !important;
      flex-shrink: 0;
      transition: all 0.15s ease;

      &:hover {
        background: #F1F5F9;
        color: #1E293B;
      }

      &.active {
        background: #EFF6FF;
        color: #2563EB;
        font-weight: 600;

        .tab-badge {
          background: #2563EB;
          color: #FFFFFF;
        }
      }

      .tab-badge {
        font-size: 11px;
        padding: 1px 6px;
        border-radius: 10px;
        background: #E2E8F0;
        color: #64748B;
        font-family: ui-monospace, monospace;
        transition: all 0.2s;
      }
    }
  }

  .filter-right-tools {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    flex-wrap: nowrap !important;
  }
}
</style>
