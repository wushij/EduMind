<template>
  <div class="assignment-filter-card module-page-filter">
    <div class="filter-row">
      <div class="capsule-search-box">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          :value="keyword"
          type="text"
          class="capsule-search-input"
          placeholder="搜索作业标题..."
          @input="emit('update:keyword', ($event.target as HTMLInputElement).value)"
          @keyup.enter="emit('search')"
        />
        <button
          v-if="keyword"
          type="button"
          class="clear-btn"
          @click="emit('update:keyword', ''); emit('search');"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <el-select
        :model-value="selectedCourseId"
        placeholder="所属课程"
        clearable
        class="filter-select"
        @update:model-value="emit('update:selectedCourseId', $event)"
        @change="emit('search')"
      >
        <el-option label="全部课程" :value="null" />
        <el-option
          v-for="c in courses"
          :key="c.id"
          :label="c.title || c.name"
          :value="c.id"
        />
      </el-select>
      <el-select
        :model-value="selectedStatus"
        placeholder="作业状态"
        clearable
        class="filter-select"
        @update:model-value="emit('update:selectedStatus', $event)"
        @change="emit('search')"
      >
        <el-option label="全部状态" value="" />
        <el-option label="进行中" value="PUBLISHED" />
        <el-option label="已归档" value="CLOSED" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Close } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';

defineProps<{
  keyword: string;
  selectedCourseId: number | null;
  selectedStatus: string;
  courses: Course[];
}>();

const emit = defineEmits<{
  'update:keyword': [value: string];
  'update:selectedCourseId': [value: number | null];
  'update:selectedStatus': [value: string];
  search: [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.assignment-filter-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px 20px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.capsule-search-box {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 220px;
  max-width: 420px;
  height: 38px;
  padding: 0 14px;
  border-radius: 9999px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  transition: all 0.2s;

  &:focus-within {
    border-color: #1677ff;
    box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.14);
  }

  .search-icon {
    font-size: 14px;
    color: #94a3b8;
    margin-right: 8px;
    flex-shrink: 0;
  }

  .capsule-search-input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 13px;
    color: #1e293b;
    min-width: 0;

    &::placeholder {
      color: #94a3b8;
    }
  }

  .clear-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
    cursor: pointer;
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;

    &:hover {
      color: #64748b;
    }
  }
}

.filter-select {
  width: 180px;
}
</style>
