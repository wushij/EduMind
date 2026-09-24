<template>
  <header class="lesson-hero">
    <div class="hero-main">
      <p v-if="parentTitle" class="breadcrumb">{{ parentTitle }}</p>
      <h1 class="lesson-title">{{ title }}</h1>
      <div class="meta-row">
        <span v-if="durationMinutes" class="pill-badge">
          <el-icon><Clock /></el-icon>
          {{ durationMinutes }} 分钟
        </span>
        <span v-if="lessonType" class="pill-badge pill-badge--type">{{ typeLabel }}</span>
        <span v-if="contentStatus" class="pill-badge" :class="statusClass">{{ statusLabel }}</span>
      </div>
      <p v-if="description" class="lesson-desc">{{ description }}</p>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Clock } from '@element-plus/icons-vue';

const props = defineProps<{
  title: string;
  parentTitle?: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  contentStatus?: string;
}>();

const typeLabel = computed(() => {
  const t = (props.lessonType || '').toUpperCase();
  if (t === 'QUIZ') return '智能自测';
  if (t === 'PRACTICE') return '实战演练';
  return '讲义精讲';
});

const statusLabel = computed(() =>
  props.contentStatus === 'PUBLISHED' ? '已发布' : '草稿'
);

const statusClass = computed(() =>
  props.contentStatus === 'PUBLISHED' ? 'pill-badge--published' : 'pill-badge--draft'
);
</script>

<style scoped lang="scss">
.lesson-hero {
  margin-bottom: 24px;
}

.hero-main {
  padding: 28px 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 55%, #ffffff 100%);
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.04);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.breadcrumb {
  margin: 0 0 8px;
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.lesson-title {
  margin: 0 0 14px;
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 0.8rem;
  font-weight: 500;

  &--type {
    background: #dbeafe;
    color: #1d4ed8;
  }

  &--published {
    background: #dcfce7;
    color: #15803d;
  }

  &--draft {
    background: #fef3c7;
    color: #b45309;
  }
}

.lesson-desc {
  margin: 0 auto;
  max-width: 860px;
  color: #475569;
  line-height: 1.7;
  font-size: 0.92rem;
  text-align: center;
}
</style>
