<template>
  <PageHeroBanner
    title="创建新课程空间"
    subtitle="全链路配置课程基准档案、初始化教学大纲与微课结构，无缝挂载 RAG 专业知识库与专属 AI 助教"
    background-variant="course"
    size="large"
  >
    <template #toolbar>
      <button type="button" class="capsule-back-btn" @click="$emit('back')">
        <svg viewBox="0 0 24 24" class="back-arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
        <span>返回课程中心</span>
      </button>
      <span class="header-guide-badge">
        <svg viewBox="0 0 24 24" class="sparkle-svg" fill="currentColor">
          <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
        </svg>
        <span>AI-Native 智能教学空间初始化向导</span>
      </span>
    </template>

    <template #extra>
      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-primary">5 步</span>
          <span class="stat-label">全链路初始化配置</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">RAG</span>
          <span class="stat-label">专业知识库挂载</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-info">AI 助教</span>
          <span class="stat-label">专属人格与流式问答</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning">实时预览</span>
          <span class="stat-label">课程卡片即时渲染</span>
        </div>
      </div>
    </template>
  </PageHeroBanner>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';

defineEmits<{ back: [] }>();
</script>

<style scoped lang="scss">
.capsule-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 16px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);

  .back-arrow-svg {
    width: 15px;
    height: 15px;
  }

  &:hover {
    color: #1677ff;
    border-color: #93c5fd;
    background: #fff;
    transform: translateX(-2px);
  }
}

.header-guide-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #c7d2fe;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 600;
  color: #4f46e5;

  .sparkle-svg {
    width: 14px;
    height: 14px;
    color: #4f46e5;
    flex-shrink: 0;
  }
}

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
</style>
