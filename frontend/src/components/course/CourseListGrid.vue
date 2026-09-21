<template>
  <!-- 课程卡片网格列表 (真实数据 / 骨架屏 / 空状态) -->
  <div class="course-grid-container">
    <!-- 1. 真实课程卡片列表 (数据就绪) -->
    <div v-if="courses.length > 0" class="course-grid" :class="{ 'is-refreshing': loading }">
      <CourseCard
        v-for="course in courses"
        :key="course.id"
        :course="course"
      />
    </div>

    <!-- 2. 骨架屏占位 (首次加载中，布局与卡片 1:1 保持一致，彻底杜绝高度坍塌与闪烁) -->
    <div v-else-if="loading" class="course-grid course-grid--skeleton">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <div class="skeleton-cover"></div>
        <div class="skeleton-body">
          <div class="skeleton-title"></div>
          <div class="skeleton-desc"></div>
          <div class="skeleton-desc skeleton-desc--short"></div>
          <div class="skeleton-footer">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-name"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 优雅空状态 (仅在加载完毕且无数据时展示) -->
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
// 3. 课程卡片网格（高度随内容，避免列表页底部大块留白）
.course-grid-container {
  min-height: 0;

  .course-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 22px;
    transition: opacity 0.2s ease;

    &.is-refreshing {
      opacity: 0.85;
    }
  }

  .skeleton-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    overflow: hidden;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    flex-direction: column;

    .skeleton-cover {
      width: 100%;
      aspect-ratio: 16 / 9;
      background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
      background-size: 200% 100%;
      animation: skeleton-shimmer 1.5s infinite;
    }

    .skeleton-body {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;

      .skeleton-title {
        height: 20px;
        width: 70%;
        border-radius: 6px;
        background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
        background-size: 200% 100%;
        animation: skeleton-shimmer 1.5s infinite;
      }

      .skeleton-desc {
        height: 14px;
        width: 95%;
        border-radius: 4px;
        background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
        background-size: 200% 100%;
        animation: skeleton-shimmer 1.5s infinite;

        &--short {
          width: 60%;
        }
      }

      .skeleton-footer {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-top: 10px;

        .skeleton-avatar {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
          background-size: 200% 100%;
          animation: skeleton-shimmer 1.5s infinite;
        }

        .skeleton-name {
          height: 14px;
          width: 80px;
          border-radius: 4px;
          background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
          background-size: 200% 100%;
          animation: skeleton-shimmer 1.5s infinite;
        }
      }
    }
  }

  @keyframes skeleton-shimmer {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }

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
