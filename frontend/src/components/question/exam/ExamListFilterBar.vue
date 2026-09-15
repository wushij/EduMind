<template>
  <div class="exam-filter-card">
    <div class="filter-row">
      <span class="filter-label">所属课程：</span>
      <div class="pill-tags-track">
        <span
          v-for="c in courseOptions"
          :key="String(c.value)"
          class="filter-pill-tag"
          :class="{ active: selectedCourseId === c.value }"
          @click="emit('course-filter', c.value)"
        >
          {{ c.label }}
        </span>
      </div>
    </div>

    <div class="filter-row filter-row--bottom">
      <div class="filter-right-search filter-right-search--full">
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
.exam-filter-card {
  background: #FFFFFF;
  border-radius: 18px;
  padding: 18px 24px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);
  display: flex;
  flex-direction: column;
  gap: 14px;

  .filter-row {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;

    .filter-label {
      font-size: 13px;
      font-weight: 600;
      color: #64748B;
      min-width: 70px;
    }

    .pill-tags-track {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
      min-height: 32px;

      .filter-pill-tag {
        padding: 4px 14px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        color: #475569;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          color: #1677FF;
          border-color: #BFDBFE;
        }

        &.active {
          background: #1677FF;
          color: #FFFFFF;
          border-color: #1677FF;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
        }
      }
    }

    &--bottom {
      justify-content: space-between;
      margin-top: 4px;
      padding-top: 14px;
      border-top: 1px solid #F1F5F9;

      .filter-right-search--full {
        width: 100%;
        display: flex;
        justify-content: flex-end;

        .capsule-search-box {
          display: flex;
          align-items: center;
          width: 320px;
          height: 38px;
          padding: 0 14px;
          border-radius: 9999px;
          border: 1.5px solid #E2E8F0;
          background: #FFFFFF;
          transition: all 0.2s;

          &:focus-within {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.14);
          }

          .search-icon {
            font-size: 13px;
            color: #94A3B8;
            margin-right: 8px;
          }

          .capsule-search-input {
            flex: 1;
            border: none;
            outline: none;
            font-size: 13px;
            color: #1E293B;

            &::placeholder {
              color: #94A3B8;
            }
          }

          .clear-btn {
            background: transparent;
            border: none;
            color: #94A3B8;
            cursor: pointer;
            font-size: 12px;
          }
        }
      }
    }
  }
}
</style>
