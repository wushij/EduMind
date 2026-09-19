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
import { Search } from '@element-plus/icons-vue';
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

.filter-select {
  width: 180px;
}
</style>
