<template>
  <header class="lesson-hero">
    <button type="button" class="back-btn" @click="emit('back')">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回大纲</span>
    </button>
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
import { ArrowLeft, Clock } from '@element-plus/icons-vue';

const props = defineProps<{
  title: string;
  parentTitle?: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  contentStatus?: string;
}>();

const emit = defineEmits<{ (e: 'back'): void }>();

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

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: #475569;
  cursor: pointer;
  margin-bottom: 12px;
}

.hero-main {
  padding: 20px 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 55%, #ffffff 100%);
  border: 1px solid #e2e8f0;
}

.breadcrumb {
  margin: 0 0 6px;
  font-size: 0.82rem;
  color: #64748b;
}

.lesson-title {
  margin: 0 0 12px;
  font-size: 1.6rem;
  color: #0f172a;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 0.78rem;

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
  margin: 0;
  color: #475569;
  line-height: 1.6;
}
</style>
