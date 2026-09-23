<template>
  <div class="dashboard-hero">
    <div class="dashboard-hero__frame">
      <img
        class="dashboard-hero__image"
        :src="dashboardBannerImg"
        alt="EduMind 智教云"
        draggable="false"
      />

      <!-- 1. 立即体验主按钮：按产品首页主视觉背景banner.png精确对齐 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--primary"
        aria-label="立即体验"
        @click="handlePrimary"
      />

      <!-- 2. 底部 4 个核心功能快捷卡片 -->
      <!-- AI 工具广场 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-1"
        aria-label="AI 工具广场"
        @click="router.push('/ai/marketplace')"
      />
      <!-- 课程 AI 助手 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-2"
        aria-label="课程 AI 助手"
        @click="router.push('/course/ai-assistant')"
      />
      <!-- AI 出题 / 智能练习 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-3"
        aria-label="AI 出题与组卷"
        @click="goQuestionModule"
      />
      <!-- AI 批改 / 学情报告 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-4"
        aria-label="AI 批改"
        @click="goGradingModule"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth/auth';
import dashboardBannerImg from '@/assets/images/产品首页主视觉背景banner.png';

const emit = defineEmits<{
  (e: 'primary'): void;
  (e: 'video'): void;
}>();

const router = useRouter();
const authStore = useAuthStore();

/** 教师与管理员端权限判断 */
const isTeacherSide = computed(() => authStore.hasAnyRole(['ADMIN', 'TEACHER']));

function handlePrimary() {
  if (isTeacherSide.value) {
    emit('primary');
  } else {
    router.push('/learning/practice');
  }
}

function goQuestionModule() {
  router.push(isTeacherSide.value ? '/ai/question/generate' : '/learning/practice');
}

function goGradingModule() {
  router.push(isTeacherSide.value ? '/ai/grading' : '/learning/report');
}
</script>

<style scoped lang="scss">
.dashboard-hero {
  width: 100%;
  margin-bottom: 0;

  &__frame {
    position: relative;
    width: 100%;
    aspect-ratio: 2172 / 724;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 6px 24px rgba(30, 80, 150, 0.08);
    background: #eef6ff;
  }

  &__image {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    display: block;
    user-select: none;
    pointer-events: none;
    object-fit: cover;
  }

  &__hitbox {
    position: absolute;
    border: none;
    padding: 0;
    margin: 0;
    background: transparent;
    cursor: pointer;
    z-index: 2;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      background: rgba(37, 99, 235, 0.08);
    }
  }

  /* 立即体验按钮点击区 */
  &__hitbox--primary {
    left: 6.031%;
    top: 62.017%;
    width: 15.654%;
    height: 10.083%;
    border-radius: 999px;

    &:hover {
      background: rgba(255, 255, 255, 0.16);
      box-shadow: 0 4px 16px rgba(37, 99, 235, 0.25);
    }

    &:active {
      transform: scale(0.98);
    }
  }

  /* 底部 4 功能卡片点击区 */
  &__hitbox--card-1 {
    left: 5.755%;
    top: 77.348%;
    width: 9.484%;
    height: 10.359%;
    border-radius: 12px;
  }

  &__hitbox--card-2 {
    left: 15.239%;
    top: 77.348%;
    width: 10.497%;
    height: 10.359%;
    border-radius: 12px;
  }

  &__hitbox--card-3 {
    left: 25.737%;
    top: 77.348%;
    width: 10.912%;
    height: 10.359%;
    border-radius: 12px;
  }

  &__hitbox--card-4 {
    left: 36.648%;
    top: 77.348%;
    width: 10.313%;
    height: 10.359%;
    border-radius: 12px;
  }
}
</style>
