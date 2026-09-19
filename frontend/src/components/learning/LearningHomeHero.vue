<template>
  <PageHeroBanner
    title="我的学习"
    subtitle="基于学习轨迹与知识掌握情况，为你推荐今日任务与巩固练习"
    background-variant="learning"
    size="large"
  >
    <template #toolbar>
      <el-select
        v-if="courseOptions.length"
        :model-value="selectedCourseId ?? undefined"
        placeholder="主修课程"
        style="width: 200px"
        @change="emit('course-change', $event)"
      >
        <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/learning/tasks')">
        <el-icon class="btn-icon"><List /></el-icon>
        <span>学习任务</span>
      </button>
      <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/learning/wrong-questions')">
        <el-icon class="btn-icon"><Warning /></el-icon>
        <span>错题本</span>
      </button>
      <button type="button" class="capsule-btn capsule-btn--primary" @click="router.push('/learning/practice')">
        <el-icon class="btn-icon"><MagicStick /></el-icon>
        <span>AI 练习</span>
      </button>
    </template>

    <template #extra>
      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-primary">{{ stats.progressPercent }}%</span>
          <span class="stat-label">课程进度</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-info">{{ stats.studyHours }} 小时</span>
          <span class="stat-label">学习时长</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">{{ stats.completedTasks }} / {{ stats.totalTasks }}</span>
          <span class="stat-label">完成任务</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning">{{ stats.masteryPercent }}%</span>
          <span class="stat-label">知识点掌握度</span>
        </div>
      </div>
    </template>
  </PageHeroBanner>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { List, Warning, MagicStick } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';

defineProps<{
  stats: {
    progressPercent: number;
    studyHours: string;
    completedTasks: number;
    totalTasks: number;
    masteryPercent: number;
  };
  courseOptions: Array<{ id: number; name: string }>;
  selectedCourseId: number | null;
}>();

const emit = defineEmits<{
  'course-change': [courseId: number];
}>();

const router = useRouter();
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
  flex-wrap: wrap;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    padding: 8px 20px;
    border-radius: 9999px;
    border: 1.5px solid rgba(22, 119, 255, 0.12);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
    display: flex;
    align-items: center;
    gap: 10px;

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

.capsule-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 18px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.22s ease;
  white-space: nowrap;

  .btn-icon {
    font-size: 16px;
  }

  &--primary {
    background: #1677ff;
    color: #fff;
    border: none;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

    &:hover {
      background: #4096ff;
    }
  }

  &--default {
    background: #fff;
    color: #334155;
    border: 1px solid #e2e8f0;

    &:hover {
      border-color: #1677ff;
      color: #1677ff;
    }
  }
}
</style>
