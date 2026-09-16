<template>
  <div class="memory-scope-tabs">
    <div class="scope-label-box">
      <el-icon class="scope-icon"><FolderOpened /></el-icon>
      <span class="label-title">认知命名空间：</span>
    </div>

    <div class="pill-segmented-container">
      <button
        type="button"
        class="scope-pill-btn"
        :class="{ active: selectedCourseId === undefined }"
        @click="$emit('switch-course', undefined)"
      >
        <span class="pill-name">全局认知底座 (跨学科)</span>
        <span class="pill-badge">{{ globalItemCount }}</span>
      </button>

      <button
        v-for="space in courseSpaces"
        :key="space.namespaceId"
        type="button"
        class="scope-pill-btn"
        :class="{ active: selectedCourseId === space.courseId }"
        @click="$emit('switch-course', space.courseId)"
      >
        <span class="pill-name">{{ space.courseTitle }}</span>
        <span class="pill-badge" :class="{ 'has-items': space.itemCount > 0 }">{{ space.itemCount }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { FolderOpened } from '@element-plus/icons-vue';
import type { MemorySpaceItemVO } from '@/types/ai/memory';

const props = defineProps<{
  spaces: MemorySpaceItemVO[];
  selectedCourseId: number | undefined;
}>();

defineEmits<{
  (e: 'switch-course', courseId?: number): void;
}>();

const globalItemCount = computed(() => {
  const g = props.spaces.find((s) => s.scope === 'GLOBAL' || s.courseId == null);
  return g ? g.itemCount : 0;
});

const courseSpaces = computed(() => {
  return props.spaces.filter((s) => s.courseId != null);
});
</script>

<style scoped lang="scss">
.memory-scope-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .scope-label-box {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #475569;
    font-size: 13px;
    font-weight: 600;

    .scope-icon {
      font-size: 16px;
      color: #2563EB;
    }

    .label-title {
      white-space: nowrap;
    }
  }

  .pill-segmented-container {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: #F1F5F9;
    border-radius: 9999px;
    padding: 4px;
    border: 1px solid #E2E8F0;
    max-width: 100%;
    overflow-x: auto;

    &::-webkit-scrollbar {
      display: none;
    }

    .scope-pill-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      color: #64748B;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      white-space: nowrap;

      &:hover {
        color: #1E293B;
        background: rgba(255, 255, 255, 0.5);
      }

      &.active {
        background: #FFFFFF;
        color: #2563EB;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);

        .pill-badge {
          background: #EFF6FF;
          color: #2563EB;
          font-weight: 700;
        }
      }

      .pill-name {
        line-height: 1.2;
      }

      .pill-badge {
        padding: 1px 7px;
        border-radius: 9999px;
        font-size: 11px;
        background: #E2E8F0;
        color: #64748B;
        transition: all 0.2s ease;

        &.has-items {
          background: #DBEAFE;
          color: #1D4ED8;
          font-weight: 600;
        }
      }
    }
  }
}
</style>
