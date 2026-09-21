<template>
  <div class="learning-home-container" v-loading="loading">
    <LearningHomeHero
      :stats="heroStats"
      :course-options="courseOptions"
      :selected-course-id="primaryCourseId"
      @course-change="onCourseChange"
    />

    <el-empty
      v-if="!loading && !hasEnrolledCourses"
      description="你还没有加入任何课程，先去课程中心选课吧"
    >
      <el-button type="primary" @click="router.push('/course')">前往课程中心</el-button>
    </el-empty>

    <div v-else class="learning-main-split">
      <LearningHomeLeftColumn
        :loading="loading"
        :weak-points="weakPoints"
        :today-tasks="todayTasks"
        :completion-rate="completionRate"
        @generate-weak="handleGenerateWeakQuestions"
        @study-point="handleStudyPoint"
        @execute-task="handleExecuteTask"
      />
      <LearningHomeRecommendationsPanel
        :loading="recommendationsLoading"
        :top-recommendations="topRecommendations"
        @start-recommendation="handleStartRecommendation"
        @discuss-ai="handleDiscussAI"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import LearningHomeHero from '@/components/learning/LearningHomeHero.vue';
import LearningHomeLeftColumn from '@/components/learning/LearningHomeLeftColumn.vue';
import LearningHomeRecommendationsPanel from '@/components/learning/LearningHomeRecommendationsPanel.vue';
import { useRecommendations } from '@/composables/learning/useRecommendations';
import { useLearningHome } from '@/composables/learning/useLearningHome';
import { useQuestionAiTutor } from '@/composables/question/useQuestionAiTutor';
import { resolveCourseRoute } from '@/utils/learning/course-route';
import { toTutorQuestion } from '@/utils/learning/map-recommendation';
import type { RecommendationItem } from '@/types/learning/recommendation';
import type { LearningHomeTaskUI, LearningHomeWeakPointUI } from '@/types/learning/home';

const router = useRouter();
const { openQuestionAiTutor } = useQuestionAiTutor();
const {
  loading,
  heroStats,
  weakPoints,
  todayTasks,
  completionRate,
  courseOptions,
  primaryCourseId,
  hasEnrolledCourses,
  refresh,
  changePrimaryCourse
} = useLearningHome();

const { items, loading: recommendationsLoading, fetchRecommendations } = useRecommendations();

onMounted(() => {
  void refresh().then(() => {
    if (primaryCourseId.value != null) {
      void fetchRecommendations(primaryCourseId.value);
    }
  });
});

watch(primaryCourseId, (cid) => {
  if (cid != null) {
    void fetchRecommendations(cid);
  }
});

const topRecommendations = computed(() => items.value.slice(0, 2));

function onCourseChange(courseId: number) {
  void changePrimaryCourse(courseId);
}

/**
 * 学生端「立即强化 / 一键生成巩固练习」统一入口：
 * 进入 AI 练习并锁定该薄弱考点（mode=KNOWLEDGE_TIER + knowledgePointId），到页面自动开练。
 * 注意：/ai/question/generate 是教师模块（meta.roles = ADMIN/TEACHER），学生点会被路由守卫拦下报「权限不足」。
 */
function goPracticeForWeakPoint(point: LearningHomeWeakPointUI) {
  router.push({
    path: '/learning/practice',
    query: {
      courseId: String(point.courseId),
      knowledgePointId: String(point.knowledgePointId),
      mode: 'KNOWLEDGE_TIER',
      autoStart: '1'
    }
  });
}

function handleGenerateWeakQuestions() {
  const point = weakPoints.value[0];
  if (!point) {
    router.push('/learning/practice');
    return;
  }
  goPracticeForWeakPoint(point);
}

function handleStudyPoint(point: LearningHomeWeakPointUI) {
  goPracticeForWeakPoint(point);
}

function handleExecuteTask(task: LearningHomeTaskUI) {
  if (task.targetUrl) {
    router.push(task.targetUrl);
    return;
  }
  if (task.type === 'ASSIGNMENT') {
    router.push('/learning/tasks');
  } else {
    router.push({ path: '/learning/practice', query: { courseId: task.courseId } });
  }
}

function handleStartRecommendation(item: RecommendationItem) {
  const courseId = item.courseId ?? primaryCourseId.value;
  if (item.type === 'exercise') {
    router.push({
      path: '/learning/practice',
      query: courseId != null ? { courseId } : undefined
    });
  } else if (courseId != null) {
    router.push(resolveCourseRoute(courseId, 'resources'));
  }
}

/**
 * 助教答疑：打开全局侧边栏 AI 并锚定该推荐项（与题库列表「AI 辅导」同一能力）。
 * 题目与资料都锚定到侧边栏，避免与「在线研读」跳到同一个资源页。
 */
function handleDiscussAI(item: RecommendationItem) {
  openQuestionAiTutor(toTutorQuestion(item));
}
</script>

<style scoped lang="scss">
.learning-home-container {
  display: flex;
  flex-direction: column;
  gap: 22px;
  width: 100%;

  .learning-main-split {
    display: grid;
    grid-template-columns: 58% 42%;
    gap: 20px;
  }
}

@media (max-width: 1024px) {
  .learning-main-split {
    grid-template-columns: 1fr !important;
  }
}
</style>
