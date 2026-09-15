<template>
  <!-- 课程卡片网格列表 (加载中 / 内容 / 空状态) -->
  <div v-loading="loading" class="course-grid-container">
    <div v-if="courses.length > 0" class="course-grid">
      <CourseCard
        v-for="course in courses"
        :key="course.id"
        :course="course"
      />
    </div>

    <!-- 优雅空状态 -->
    <div v-else class="empty-state-wrapper">
      <div class="empty-icon-box">
        <svg viewBox="0 0 24 24" class="empty-svg" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          <line x1="9" y1="9" x2="15" y2="9"></line>
          <line x1="9" y1="13" x2="13" y2="13"></line>
        </svg>
      </div>
      <h4 class="empty-title">暂无符合条件的课程</h4>
      <p class="empty-desc">换个关键词搜索试试，或点击下方重置筛选</p>
      <button type="button" class="capsule-btn capsule-btn--default" @click="emit('reset-filters')">
        重置全部筛选
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import CourseCard from '@/components/course/CourseCard.vue';
import type { Course } from '@/types/course/course';

defineProps<{
  loading: boolean;
  courses: Course[];
}>();

const emit = defineEmits<{
  'reset-filters': [];
}>();
</script>

<style scoped lang="scss">
// 3. 课程卡片网格
.course-grid-container {
  min-height: 420px;

  .course-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 22px;
  }

  .empty-state-wrapper {
    padding: 60px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #EBF1F7;

    .empty-icon-box {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: #F1F5F9;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #94A3B8;
      margin-bottom: 16px;

      .empty-svg {
        width: 32px;
        height: 32px;
      }
    }

    .empty-title {
      margin: 0 0 8px 0;
      font-size: 16px;
      font-weight: 600;
      color: #1E293B;
    }

    .empty-desc {
      margin: 0 0 20px 0;
      font-size: 13px;
      color: #94A3B8;
    }
  }
}

// 长圆按钮统一样式
.capsule-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 20px;
  border-radius: 9999px; // 纯正长圆
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  .btn-icon-svg {
    width: 16px;
    height: 16px;
  }

  &--primary {
    background: #1677FF;
    color: #FFFFFF;
    border: none;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

    &:hover {
      background: #4096FF;
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
    }

    &:active {
      background: #0958D9;
      transform: translateY(0);
    }
  }

  &--default {
    background: #FFFFFF;
    color: #334155;
    border: 1px solid #E2E8F0;

    &:hover {
      border-color: #CBD5E1;
      color: #1677FF;
      background: #F8FAFC;
      transform: translateY(-1px);
    }
  }
}
</style>
