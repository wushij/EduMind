<template>
  <div class="filter-card">
    <div class="filter-left">
      <el-input
        :model-value="searchKeyword"
        placeholder="搜索租户名称、学校编码或绑定域名..."
        prefix-icon="Search"
        clearable
        class="pill-search-input"
        @update:model-value="$emit('update:searchKeyword', $event)"
        @input="$emit('search')"
      />

      <div class="pill-segmented-control">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="pill-tab-item"
          :class="{ 'is-active': statusFilter === tab.value }"
          @click="$emit('status-tab-change', tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <el-select
        :model-value="planFilter"
        placeholder="套餐方案"
        clearable
        class="pill-select"
        style="width: 130px;"
        @update:model-value="$emit('update:planFilter', $event)"
        @change="$emit('search')"
      >
        <el-option label="全部套餐" value="" />
        <el-option label="旗舰版" value="FLAGSHIP" />
        <el-option label="专业版" value="PRO" />
        <el-option label="标准版" value="STANDARD" />
      </el-select>
    </div>

    <div class="filter-right">
      <el-button type="primary" class="gradient-btn pill-btn" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
        <span>入驻新学校/租户</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue';

interface StatusTab {
  label: string;
  value: number | '';
}

defineProps<{
  searchKeyword: string;
  statusFilter: number | '';
  planFilter: string;
  statusTabs: StatusTab[];
}>();

defineEmits<{
  'update:searchKeyword': [value: string];
  'update:planFilter': [value: string];
  'status-tab-change': [value: number | ''];
  search: [];
  create: [];
}>();
</script>

<style scoped lang="scss">
.filter-card {
  background: #FFFFFF;
  border-radius: 24px;
  border: 1.5px solid rgba(226, 232, 240, 0.9);
  padding: 14px 20px;
  margin-bottom: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);

  .filter-left {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;

    .pill-search-input {
      width: 320px;

      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        padding-left: 14px;
        box-shadow: 0 0 0 1px #CBD5E1 inset;

        &:hover, &.is-focus {
          box-shadow: 0 0 0 1.5px #1677FF inset;
        }
      }
    }

    .pill-segmented-control {
      display: flex;
      background: #F1F5F9;
      border-radius: 9999px;
      padding: 3px;
      gap: 2px;

      .pill-tab-item {
        border: none;
        background: transparent;
        padding: 6px 16px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;
        color: #64748B;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          color: #1E293B;
        }

        &.is-active {
          background: #FFFFFF;
          color: #1677FF;
          box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
        }
      }
    }

    .pill-select {
      :deep(.el-select__wrapper) {
        border-radius: 9999px;
      }
    }
  }

  .filter-right {
    .pill-btn {
      border-radius: 9999px;
      font-weight: 600;
      padding: 10px 22px;

      &.gradient-btn {
        background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
        border: none;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);

        &:hover {
          opacity: 0.92;
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
        }
      }
    }
  }
}
</style>
