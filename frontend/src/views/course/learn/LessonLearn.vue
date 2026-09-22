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
        @back="goChapters"
      />

      <div class="lesson-layout">
        <main class="lesson-main">
          <LessonBlockRenderer
            v-if="content.blocks.length"
            :blocks="content.blocks"
            :knowledge-points="lesson.knowledgePoints"
            :resources="lesson.resources"
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

const tocItems = ref<LessonTocItem[]>([]);

watch(
  () => content.value.blocks,
  blocks => {
    tocItems.value = buildLessonToc(blocks);
  },
  { immediate: true, deep: true }
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

/**
 * 顶部介绍与正文左列同宽（1200 - 300 目录列 - 20 间距 = 880）。
 * 否则 hero 通栏 1200px、正文卡片只有 880px，会出现"顶部边框长、内容边框短"的错位。
 */
.lesson-hero-block {
  width: calc(100% - 320px);
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
</style>
