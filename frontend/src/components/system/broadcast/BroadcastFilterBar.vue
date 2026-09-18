<template>
  <div class="filter-card system-filter-card">
    <div class="filter-left">
      <el-input
        :model-value="searchKeyword"
        placeholder="搜索广播标题或正文内容..."
        clearable
        :prefix-icon="Search"
        class="filter-search-input"
        @update:model-value="$emit('update:searchKeyword', $event)"
        @clear="$emit('search')"
        @keyup.enter="$emit('search')"
      />

      <el-select
        :model-value="targetTypeFilter"
        placeholder="全部受众"
        clearable
        style="width: 150px"
        @update:model-value="$emit('update:targetTypeFilter', $event)"
        @change="$emit('search')"
      >
        <el-option label="全部受众" value="" />
        <el-option label="全体用户" value="all" />
        <el-option label="按角色" value="role" />
      </el-select>

      <el-select
        :model-value="priorityFilter"
        placeholder="全部级别"
        clearable
        style="width: 150px"
        @update:model-value="$emit('update:priorityFilter', $event)"
        @change="$emit('search')"
      >
        <el-option label="全部级别" value="" />
        <el-option label="普通广播" :value="0" />
        <el-option label="重要弹窗" :value="1" />
        <el-option label="紧急公告" :value="2" />
      </el-select>

      <el-button
        type="primary"
        round
        :icon="Search"
        class="btn-round-search"
        :disabled="loading"
        @click="$emit('search')"
      >
        查询
      </el-button>
      <el-button
        round
        class="btn-round-reset"
        :disabled="loading"
        @click="$emit('reset')"
      >
        <el-icon class="mr-1" :class="{ 'is-loading': loading }"><Refresh /></el-icon>
        <span>重置</span>
      </el-button>
    </div>

    <div class="filter-right">
      <span class="total-badge">共找到 {{ totalCount }} 条广播</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue';

defineProps<{
  searchKeyword: string;
  targetTypeFilter: string;
  priorityFilter: number | string;
  loading: boolean;
  totalCount: number;
}>();

defineEmits<{
  'update:searchKeyword': [value: string];
  'update:targetTypeFilter': [value: string];
  'update:priorityFilter': [value: number | string];
  search: [];
  reset: [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.filter-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: none;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-search-input {
  width: 250px;
}

.btn-round-search {
  border-radius: 9999px !important;
  padding: 8px 18px;
}

.btn-round-reset {
  border-radius: 9999px !important;
  padding: 8px 18px;
}

.total-badge {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}
</style>
