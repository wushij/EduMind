<template>
  <div class="lesson-preview-pane">
    <header v-if="title || description" class="preview-header">
      <h1 v-if="title" class="preview-title">{{ title }}</h1>
      <p v-if="description" class="preview-subtitle">{{ description }}</p>
      <div v-if="metaTags.length" class="preview-meta-row">
        <span v-for="tag in metaTags" :key="tag" class="meta-pill">{{ tag }}</span>
      </div>
    </header>

    <div v-if="objectiveBody?.trim()" class="preview-objective">
      <h4 class="objective-title">{{ objectiveTitle || '学习目标' }}</h4>
      <div
        class="markdown-body assistant-markdown-surface chat-md-content"
        v-html="objectiveHtml"
      />
    </div>

    <div
      ref="bodyRef"
      class="markdown-body assistant-markdown-surface chat-md-content preview-body"
      v-html="bodyHtml"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue';
import { bindLessonMarkdownEnhancements, renderLessonMarkdown } from '@/utils/format/lesson-markdown';

const props = defineProps<{
  markdown: string;
  title?: string;
  description?: string;
  durationMinutes?: number;
  lessonType?: string;
  objectiveTitle?: string;
  objectiveBody?: string;
}>();

const bodyRef = ref<HTMLElement | null>(null);

const bodyHtml = computed(() => renderLessonMarkdown(props.markdown || ''));
const objectiveHtml = computed(() => renderLessonMarkdown(props.objectiveBody || ''));

const metaTags = computed(() => {
  const tags: string[] = [];
  if (props.durationMinutes) tags.push(`${props.durationMinutes} 分钟`);
  const t = (props.lessonType || '').toUpperCase();
  if (t === 'LECTURE') tags.push('讲义精讲');
  else if (t === 'PRACTICE') tags.push('实战演练');
  else if (t === 'QUIZ') tags.push('智能自测');
  return tags;
});

watch(
  () => [props.markdown, props.objectiveBody],
  () => {
    nextTick(() => bindLessonMarkdownEnhancements(bodyRef.value));
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.lesson-preview-pane {
  padding: 20px 24px 32px;
  min-height: 100%;
  box-sizing: border-box;
  background: #fff;
}

.preview-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.preview-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.35;
  margin: 0 0 8px;
}

.preview-subtitle {
  margin: 0 0 12px;
  font-size: 0.9rem;
  color: #64748b;
  line-height: 1.5;
}

.preview-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
}

.preview-objective {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.objective-title {
  margin: 0 0 8px;
  font-size: 0.95rem;
  color: #166534;
}

.preview-body {
  line-height: 1.75;
}
</style>
