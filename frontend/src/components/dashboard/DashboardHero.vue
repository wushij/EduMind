<template>
  <div class="dashboard-hero">
    <div class="dashboard-hero__frame">
      <img
        class="dashboard-hero__image"
        :src="dashboardBannerImg"
        alt="EduMind 智教云"
        draggable="false"
      />

      <!-- 1. 立即体验主按钮：按产品首页主视觉背景banner.png（2172×724）实测坐标精确对齐 (x:131~471, y:449~522) -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--primary"
        aria-label="立即体验"
        @click="emit('primary')"
      />

      <!-- 2. 底部 4 个核心功能快捷卡片 (y:560~635, 顶 77.348%, 高 10.359%) -->
      <!-- AI 工具广场：x:125~331 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-1"
        aria-label="AI 工具广场"
        @click="router.push('/ai/marketplace')"
      />
      <!-- 课程 AI 助手：x:331~559 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-2"
        aria-label="课程 AI 助手"
        @click="router.push('/course/ai-assistant')"
      />
      <!-- AI 出题 / 组卷：x:559~796 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-3"
        aria-label="AI 出题与组卷"
        @click="router.push('/ai/question/generate')"
      />
      <!-- AI 批改：x:796~1020 -->
      <button
        type="button"
        class="dashboard-hero__hitbox dashboard-hero__hitbox--card-4"
        aria-label="AI 批改"
        @click="router.push('/ai/grading')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import dashboardBannerImg from '@/assets/images/产品首页主视觉背景banner.png';

const emit = defineEmits<{
  (e: 'primary'): void;
  (e: 'video'): void;
}>();

const router = useRouter();
</script>

<style scoped lang="scss">
.dashboard-hero {
  margin-bottom: 0;

  &__frame {
    position: relative;
    width: 100%;
    aspect-ratio: 2172 / 724;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.06);
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
  }

  &__hitbox {
    position: absolute;
    border: none;
    padding: 0;
    margin: 0;
    background: transparent;
    cursor: pointer;
    z-index: 2;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(22, 119, 255, 0.08);
    }
  }

  /* 立即体验：(131, 449) - (471, 522), 宽 340px, 高 73px */
  &__hitbox--primary {
    left: 6.031%;
    top: 62.017%;
    width: 15.654%;
    height: 10.083%;
    border-radius: 999px;

    &:hover {
      background: rgba(255, 255, 255, 0.16);
      box-shadow: 0 4px 16px rgba(22, 119, 255, 0.28);
    }

    &:active {
      transform: scale(0.98);
    }
  }

  /* 底部 4 功能卡片 (y: 560~635, 顶 77.348%, 高 10.359%) */
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
