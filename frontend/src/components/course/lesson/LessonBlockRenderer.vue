<template>
  <div ref="rootRef" class="lesson-blocks">
    <div
      v-for="(block, index) in blocks"
      :key="`${block.type}-${index}`"
      class="lesson-block"
      :class="`lesson-block--${block.type}`"
    >
      <h2 v-if="block.type === 'heading'" class="block-heading" :class="`level-${block.level}`">
        {{ block.text }}
      </h2>

      <div
        v-else-if="block.type === 'markdown'"
        class="markdown-body assistant-markdown-surface chat-md-content"
        v-html="renderLessonMarkdown(block.body)"
      />

      <div
        v-else-if="block.type === 'callout'"
        class="lesson-callout"
        :class="`lesson-callout--${block.variant}`"
      >
        <h4 class="callout-title">{{ block.title }}</h4>
        <div
          class="markdown-body assistant-markdown-surface chat-md-content"
          v-html="renderLessonMarkdown(block.body)"
        />
      </div>

      <LessonResourceEmbed
        v-else-if="block.type === 'resource'"
        :resource-id="block.resourceId"
        :display="block.display"
        :resources="resources"
      />

      <div v-else-if="block.type === 'knowledgePoints'" class="lesson-kp-block">
        <h4 class="kp-block-title">本课知识点</h4>
        <div class="kp-chip-row">
          <span
            v-for="kp in resolveKnowledgePoints(block.knowledgePointIds)"
            :key="kp.id"
            class="kp-chip"
          >
            {{ kp.title || kp.name }}
          </span>
        </div>
      </div>

      <div v-else-if="block.type === 'quizEntry'" class="lesson-quiz-entry">
        <el-icon><EditPen /></el-icon>
        <span>完成学习后可进行智能自测巩固</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { EditPen } from '@element-plus/icons-vue';
import type { LessonBlock } from '@/types/course/lesson-content';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import type { LessonResourceSummary } from '@/api/course/lesson';
import { renderLessonMarkdown, bindLessonMarkdownEnhancements } from '@/utils/format/lesson-markdown';
import { applyLessonTocFromDom, type LessonTocItem } from '@/utils/course/lesson-toc';
import { onMounted, watch, nextTick, ref } from 'vue';
import LessonResourceEmbed from '@/components/course/lesson/LessonResourceEmbed.vue';

const props = defineProps<{
  blocks: LessonBlock[];
  knowledgePoints?: KnowledgePoint[];
  resources?: LessonResourceSummary[];
}>();

const emit = defineEmits<{
  'toc-update': [LessonTocItem[]];
}>();

const rootRef = ref<HTMLElement | null>(null);

function resolveKnowledgePoints(ids: number[]) {
  const list = props.knowledgePoints || [];
  return ids.map(id => list.find(k => Number(k.id) === Number(id))).filter(Boolean) as KnowledgePoint[];
}

function enhance() {
  nextTick(() => {
    bindLessonMarkdownEnhancements(rootRef.value);
    const toc = applyLessonTocFromDom(rootRef.value);
    emit('toc-update', toc);
  });
}

onMounted(enhance);
watch(() => props.blocks, enhance, { deep: true });
</script>

<style scoped lang="scss">
.lesson-blocks {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.block-heading {
  margin: 0;
  font-weight: 700;
  color: #0f172a;
  scroll-margin-top: 88px;

  &.level-2 {
    font-size: 1.35rem;
  }
}

.lesson-block--callout.lesson-callout--objective {
  scroll-margin-top: 88px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(.lesson-toc-anchor) {
  scroll-margin-top: 88px;
}

.markdown-body :deep(p.lesson-toc-anchor) {
  margin-top: 1.25em;
  margin-bottom: 0.5em;
  font-weight: 700;
  color: #0f172a;
}

.lesson-callout {
  border-radius: 16px;
  padding: 16px 18px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;

  &--objective {
    border-color: #bfdbfe;
    background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  }

  .callout-title {
    margin: 0 0 8px;
    font-size: 0.95rem;
    color: #1d4ed8;
  }
}

.lesson-kp-block {
  .kp-block-title {
    margin: 0 0 10px;
    font-size: 0.9rem;
    color: #475569;
  }

  .kp-chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .kp-chip {
    padding: 6px 12px;
    border-radius: 999px;
    background: #eef2ff;
    color: #3730a3;
    font-size: 0.82rem;
  }
}

.lesson-quiz-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fffbeb;
  color: #b45309;
  font-size: 0.88rem;
}
</style>
