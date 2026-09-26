<template>
  <div class="lesson-learn-page">
    <div v-if="loading" class="lesson-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载课节内容...</span>
    </div>

    <div v-else-if="errorMessage" class="lesson-empty">
      <el-icon class="empty-icon"><WarningFilled /></el-icon>
      <h3>{{ errorMessage }}</h3>
      <p>可先使用课程 AI 助手预习本课主题。</p>
      <button type="button" class="capsule-btn" @click="goCourseAi">打开课程 AI</button>
    </div>

    <template v-else-if="lesson">
      <LessonLearnHero
        class="lesson-hero-block"
        :title="lesson.title"
        :parent-title="lesson.parentChapterTitle"
        :description="lesson.description"
        :duration-minutes="lesson.durationMinutes"
        :lesson-type="lesson.lessonType"
        :content-status="lesson.contentStatus"
      />

      <div class="lesson-layout">
        <main class="lesson-main">
          <LessonBlockRenderer
            v-if="content.blocks.length"
            :blocks="content.blocks"
            :knowledge-points="lesson.knowledgePoints"
            :resources="lesson.resources"
            :lesson-title="lesson.title"
            @toc-update="tocItems = $event"
          />
          <div v-else class="lesson-empty inline">
            <el-icon class="empty-icon"><Reading /></el-icon>
            <p v-if="preview">本课节尚未编写块式正文，请在大纲中点击「编辑内容」或「AI 生成正文」。</p>
            <p v-else>教师正在为本课节备课，正文即将上线。</p>
            <button v-if="preview" type="button" class="capsule-btn" @click="goEditLesson">编辑课节正文</button>
          </div>

        </main>

        <aside class="lesson-side">
          <LessonCompleteBar
            class="lesson-side-actions"
            :lesson-type="lesson.lessonType"
            :completed="lesson.progress?.status === 'COMPLETED'"
            @complete="markCompleted"
            @ai="goCourseAiWithContext"
            @quiz="goQuiz"
          />
          <LessonLearnToc :items="tocItems" />
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import { useRoute, useRouter } from 'vue-router';
import { Loading, WarningFilled, Reading } from '@element-plus/icons-vue';
import { useLessonLearn } from '@/composables/course/useLessonLearn';
import LessonLearnHero from '@/components/course/lesson/LessonLearnHero.vue';
import LessonBlockRenderer from '@/components/course/lesson/LessonBlockRenderer.vue';
import LessonCompleteBar from '@/components/course/lesson/LessonCompleteBar.vue';
import LessonLearnToc from '@/components/course/lesson/LessonLearnToc.vue';
import type { LessonTocItem } from '@/utils/course/lesson-toc';
import { buildLessonToc } from '@/utils/course/lesson-toc';
import { buildLessonLearnAiExcerpt } from '@/utils/course/lesson-learn-ai-context';
import { useAuthStore } from '@/stores/auth/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

/** 教师侧才有 AI 出题权限；学生点击随堂测/生成练习应走学生侧的 AI 练习 */
const isTeacherSide = computed(() => authStore.hasAnyRole(['ADMIN', 'TEACHER']));

const courseId = computed(() => Number(route.params.id));
const lessonId = computed(() => Number(route.params.lessonId));
const preview = computed(() => route.query.preview === 'true');

const { loading, lesson, content, errorMessage, markCompleted } = useLessonLearn(
  courseId.value,
  lessonId.value,
  preview.value
);

import { scrollElementIntoView } from '@/utils/dom/scroll-into-view';

const tocItems = ref<LessonTocItem[]>([]);

function tryLocateCitationTarget() {
  const hash = route.hash ? decodeURIComponent(route.hash.replace(/^#/, '')) : '';
  const anchor = ((route.query.anchor as string) || hash || '').trim();
  const excerpt = ((route.query.excerpt as string) || '').trim();
  const chunkId = (route.query.chunkId as string) || '';

  if (!anchor && !excerpt && !chunkId) return;

  // 延迟给 DOM 与 KaTeX 公式渲染留出充分时间
  setTimeout(() => {
    performScrollAndHighlight(anchor, excerpt, chunkId);
  }, 160);
}

function performScrollAndHighlight(anchor?: string, excerpt?: string, chunkId?: string) {
  const container = document.querySelector('.lesson-blocks');
  if (!container) return;

  let targetEl: HTMLElement | null = null;

  // 1. 尝试直接通过 ID 查找
  if (anchor) {
    targetEl = document.getElementById(anchor);
  }

  // 2. 尝试在目录 tocItems 中通过标题匹配 ID
  if (!targetEl && anchor && tocItems.value.length) {
    const cleanAnchor = anchor.replace(/[#*`$\\]/g, '').trim();
    const matchedToc = tocItems.value.find(item => {
      const cleanTitle = item.title.replace(/[#*`$\\]/g, '').trim();
      return (
        cleanTitle === cleanAnchor ||
        cleanTitle.includes(cleanAnchor) ||
        cleanAnchor.includes(cleanTitle)
      );
    });
    if (matchedToc) {
      targetEl = document.getElementById(matchedToc.id);
    }
  }

  // 3. 遍历标题元素 (.block-heading, .callout-title, h1-h6) 匹配文本
  if (!targetEl && anchor) {
    const cleanAnchor = anchor.replace(/[#*`$\\]/g, '').trim();
    const headings = container.querySelectorAll<HTMLElement>(
      '.block-heading, .callout-title, h1, h2, h3, h4, h5, h6'
    );
    for (const h of Array.from(headings)) {
      const text = (h.textContent || '').replace(/[#*`$\\]/g, '').trim();
      if (text && (text === cleanAnchor || text.includes(cleanAnchor) || cleanAnchor.includes(text))) {
        targetEl = h;
        break;
      }
    }
  }

  // 4. 尝试通过切片摘要片段 (excerpt) 在正文段落中模糊匹配
  if (!targetEl && excerpt) {
    const cleanExcerpt = excerpt.replace(/[#*`$\\]/g, '').trim();
    if (cleanExcerpt.length >= 4) {
      const sample = cleanExcerpt.slice(0, 16);
      const candidates = container.querySelectorAll<HTMLElement>(
        '.lesson-block p, .lesson-block li, .lesson-block blockquote, .lesson-block .markdown-body > *'
      );
      for (const el of Array.from(candidates)) {
        const text = (el.textContent || '').trim();
        if (text.includes(cleanExcerpt) || (sample.length >= 4 && text.includes(sample))) {
          targetEl = el;
          break;
        }
      }
    }
  }

  // 5. 如果找到目标元素，执行平滑滚动并施加呼吸聚焦高亮
  if (targetEl) {
    scrollElementIntoView(targetEl, 100);

    targetEl.classList.remove('citation-target-highlight');
    void targetEl.offsetWidth; // 触发 reflow 重置动画
    targetEl.classList.add('citation-target-highlight');

    setTimeout(() => {
      targetEl?.classList.remove('citation-target-highlight');
    }, 3200);
  }
}

watch(
  () => content.value.blocks,
  blocks => {
    tocItems.value = buildLessonToc(blocks, lesson.value?.title);
    if (blocks.length) {
      tryLocateCitationTarget();
    }
  },
  { immediate: true, deep: true }
);

watch(
  () => [route.hash, route.query.anchor, route.query.chunkId, route.query.excerpt],
  () => {
    tryLocateCitationTarget();
  }
);

const teachingCopilotStore = useTeachingCopilotStore();

function syncLessonLearnTeachingContext() {
  const val = lesson.value;
  if (!val) return;
  const { draftExcerpt, objectiveExcerpt } = buildLessonLearnAiExcerpt(content.value);
  teachingCopilotStore.setContext({
    contextModule: 'lesson_learn',
    courseId: courseId.value,
    lessonChapterId: lessonId.value,
    title: val.title,
    description: val.description,
    lessonType: val.lessonType,
    contentStatus: val.contentStatus,
    draftExcerpt,
    objectiveExcerpt
  });
}

watch(lesson, () => syncLessonLearnTeachingContext(), { immediate: true });
watch(() => content.value.blocks, () => syncLessonLearnTeachingContext(), { deep: true });

onUnmounted(() => {
  if (teachingCopilotStore.activeContext?.contextModule === 'lesson_learn') {
    teachingCopilotStore.clearContext();
  }
});

function goChapters() {
  router.push(`/course/${courseId.value}/chapters`);
}

function goCourseAi() {
  router.push(`/course/${courseId.value}/ai`);
}

function goCourseAiWithContext() {
  teachingCopilotStore.openAssistantWithContext(
    {
      contextModule: 'lesson_learn',
      courseId: courseId.value,
      lessonChapterId: lessonId.value,
      title: lesson.value?.title,
      description: lesson.value?.description,
      lessonType: lesson.value?.lessonType,
      contentStatus: lesson.value?.contentStatus
    },
    '',
    { autoSend: false }
  );
}

function goQuiz() {
  // AI 出题为教师模块（meta.roles = ADMIN/TEACHER）；学生侧等价能力是「AI 练习」，直接进入并自动开练
  if (isTeacherSide.value) {
    router.push(`/ai/question/generate?courseId=${courseId.value}`);
    return;
  }
  router.push({
    path: '/learning/practice',
    query: { courseId: String(courseId.value), autoStart: '1' }
  });
}

function goEditLesson() {
  router.push(`/course/${courseId.value}/lessons/${lessonId.value}/edit`);
}
</script>

<style scoped lang="scss">
.lesson-learn-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}

.lesson-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
}

.lesson-hero-block {
  width: 100%;
}

.lesson-main {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 22px;
}

.lesson-side {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 16px;
  position: sticky;
  top: 16px;
  align-self: start;
  height: fit-content;
}

.lesson-side-actions {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.lesson-loading,
.lesson-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 20px;
  color: #64748b;

  &.inline {
    padding: 32px 12px;
  }
}

.empty-icon {
  font-size: 2rem;
  color: #94a3b8;
}

.capsule-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}

@media (max-width: 960px) {
  .lesson-layout {
    grid-template-columns: 1fr;
  }

  // 单列布局下没有右侧目录列，hero 恢复通栏
  .lesson-hero-block {
    width: 100%;
  }
}

:deep(.citation-target-highlight) {
  border-radius: 8px;
  animation: citationPulseGlow 3s ease-out forwards;
  transition: all 0.3s ease;
}

:deep(.block-heading.citation-target-highlight),
:deep(.callout-title.citation-target-highlight) {
  border-left: 4px solid #2563eb;
  padding-left: 8px;
}

@keyframes citationPulseGlow {
  0% {
    background-color: rgba(37, 99, 235, 0.22);
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.2), 0 2px 12px rgba(37, 99, 235, 0.15);
  }
  30% {
    background-color: rgba(37, 99, 235, 0.15);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12), 0 2px 8px rgba(37, 99, 235, 0.1);
  }
  70% {
    background-color: rgba(37, 99, 235, 0.08);
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.05);
  }
  100% {
    background-color: transparent;
    box-shadow: none;
  }
}
</style>
