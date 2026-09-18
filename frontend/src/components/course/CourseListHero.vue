<template>
  <PageHeroBanner
    title="课程中心 · 智能教学空间管理"
    subtitle="管理与修读学科空间，全流程支持 AI 智能出题、知识库沉淀与流式助教问答"
    background-variant="course"
    size="large"
  >
    <template #toolbar>
      <button type="button" class="capsule-btn capsule-btn--default" @click="emit('join-course')">
        <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="8.5" cy="7" r="4"></circle>
          <line x1="20" y1="8" x2="20" y2="14"></line>
          <line x1="23" y1="11" x2="17" y2="11"></line>
        </svg>
        <span>加入课程</span>
      </button>

      <button
        v-if="canCreateCourse"
        type="button"
        class="capsule-btn capsule-btn--primary"
        @click="router.push('/course/create')"
      >
        <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2.2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span>创建新课程</span>
      </button>
    </template>

    <template #extra>
      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-primary">{{ total }}</span>
          <span class="stat-label">课程空间总数</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">{{ activeCourseCount }}</span>
          <span class="stat-label">进行中课程</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning">{{ archivedCourseCount }}</span>
          <span class="stat-label">已结课归档</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-info">AI 就绪</span>
          <span class="stat-label">智能助教与 RAG 全链路</span>
        </div>
      </div>
    </template>
  </PageHeroBanner>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useAuthStore } from '@/stores/auth/auth';
import { RoleEnum } from '@/constants/auth';

defineProps<{
  total: number;
  activeCourseCount: number;
  archivedCourseCount: number;
}>();

const emit = defineEmits<{
  'join-course': [];
}>();

const router = useRouter();
const authStore = useAuthStore();

const canCreateCourse = computed(() =>
  authStore.hasAnyRole([RoleEnum.ADMIN, RoleEnum.TEACHER])
);
</script>

<style scoped lang="scss">
:deep(.page-hero-banner) {
  margin-bottom: 0;
  padding-bottom: 12px;

  &.page-hero-banner--large {
    padding: 18px 28px 12px;
  }

  .hero-extra {
    margin-top: 10px;
    padding-top: 8px;
  }
}

.hero-stats-row {
  display: flex;
  gap: 10px;
  margin-top: 0;
  flex-wrap: wrap;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    padding: 6px 20px;
    border-radius: 9999px;
    border: 1.5px solid rgba(22, 119, 255, 0.12);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    display: flex;
    align-items: center;
    gap: 10px;
    transition: all 0.25s ease;

    &:hover {
      background: #fff;
      border-color: #1677ff;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
    }

    .stat-num {
      font-size: 18px;
      font-weight: 800;
      line-height: 1;

      &.text-primary { color: #2563eb; }
      &.text-success { color: #16a34a; }
      &.text-warning { color: #d97706; }
      &.text-info { color: #0284c7; }
    }

    .stat-label {
      font-size: 12.5px;
      font-weight: 500;
      color: #475569;
      white-space: nowrap;
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
