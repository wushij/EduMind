<template>
  <div class="exam-filter-card module-page-filter">
    <div class="filter-row filter-row--course-search">
      <span class="filter-label">所属课程：</span>
      <el-select
        :model-value="selectedCourseId"
        placeholder="全部课程"
        clearable
        filterable
        class="filter-select filter-select--course"
        @update:model-value="emit('course-filter', $event)"
      >
        <el-option
          v-for="c in courseOptions"
          :key="String(c.value)"
          :label="c.label"
          :value="c.value"
        />
      </el-select>

      <div class="capsule-search-box">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          :value="keyword"
          type="text"
          class="capsule-search-input"
          placeholder="搜索试卷标题、课程名称..."
          @input="emit('update:keyword', ($event.target as HTMLInputElement).value)"
          @keyup.enter="emit('search')"
        />
        <button
          v-if="keyword"
          type="button"
          class="clear-btn"
          @click="emit('clear-keyword')"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Close } from '@element-plus/icons-vue';

defineProps<{
  courseOptions: Array<{ label: string; value: number | null }>;
  selectedCourseId: number | null;
  keyword: string;
}>();

const emit = defineEmits<{
  'update:keyword': [value: string];
  'course-filter': [value: number | null];
  search: [];
  'clear-keyword': [];
}>();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.exam-filter-card {
  .filter-row {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;

    &--course-search {
      width: 100%;
    }

    .filter-label {
      font-size: 13px;
      font-weight: 600;
      color: #64748b;
      min-width: 70px;
      flex-shrink: 0;
    }

    .filter-select--course {
      width: min(280px, 100%);
      min-width: 200px;
      flex-shrink: 0;
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
        font-size: 13px;
        color: #94a3b8;
        margin-right: 8px;
        flex-shrink: 0;
      }

      .capsule-search-input {
        flex: 1;
        border: none;
        outline: none;
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
        display: flex;
        align-items: center;
        flex-shrink: 0;
      }
    }
  }
}
</style>
