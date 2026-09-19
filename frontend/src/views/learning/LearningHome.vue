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
import { resolveCourseRoute } from '@/utils/learning/course-route';
import type { RecommendationItem } from '@/types/learning/recommendation';
import type { LearningHomeTaskUI, LearningHomeWeakPointUI } from '@/types/learning/home';

const router = useRouter();
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

function handleGenerateWeakQuestions() {
  const point = weakPoints.value[0];
  if (!point) {
    router.push('/learning/practice');
    return;
  }
  router.push({
    path: '/ai/question/generate',
    query: {
      courseId: String(point.courseId),
      subject: point.course,
      knowledgePoint: point.name
    }
  });
}

function handleStudyPoint(point: LearningHomeWeakPointUI) {
  router.push({
    path: '/ai/question/generate',
    query: {
      courseId: String(point.courseId),
      subject: point.course,
      knowledgePoint: point.name
    }
  });
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

function handleDiscussAI(item: RecommendationItem) {
  const courseId = item.courseId ?? primaryCourseId.value;
  if (courseId != null) {
    router.push(resolveCourseRoute(courseId, 'ai'));
  }
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
