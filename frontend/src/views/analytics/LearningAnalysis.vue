<template>
  <div class="learning-analysis-page" v-loading="loading">
    <!-- 顶部长圆美学 Hero Banner (与课程中心同款视觉设计) -->
    <LearningAnalysisHero
      :course-id="courseId"
      :course-options="courseOptions"
      :course-name="currentCourseName"
      :course-code="currentCourseCode"
      :teacher-name="currentTeacherName"
      :semester="currentSemester"
      :student-count="learningData?.studentCount"
      :completion-rate="learningData?.completionRate"
      :avg-score="learningData?.avgScore"
      :ai-usage-count="learningData?.aiUsageCount"
      :active-tab="activeTab"
      :selected-student-name="portraitData?.studentInfo.realName"
      :range="range"
      :advice-loading="adviceLoading"
      @change-course="handleCourseChange"
      @change-range="handleRangeChange"
      @update:active-tab="handleTabChange"
      @generate-advice="handleGenerateAdvice"
    />

    <!-- Mock 降级通知 -->
    <div v-if="usedMockFallback" class="mock-fallback-pill">
      <svg viewBox="0 0 24 24" class="pill-svg" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="16" x2="12" y2="12"></line>
        <line x1="12" y1="8" x2="12.01" y2="8"></line>
      </svg>
      <span>当前数据由前端智能模拟引擎兜底呈现（服务接口离线或 VITE_USE_MOCK=true）</span>
    </div>

    <!-- 视图切换过渡容器 -->
    <transition name="fade-slide" mode="out-in">
      <!-- 维度 1: 班级整体学情分析 -->
      <OverallAnalyticsView
        v-if="activeTab === 'overall'"
        :key="`overall-${courseId}`"
        :learning-data="learningData"
        :is-aggregated="isAggregated"
        :teaching-advice="teachingAdvice"
        :advice-loading="adviceLoading"
        @view-portrait="handleViewStudentPortrait"
        @generate-advice="handleGenerateAdvice"
      />

      <!-- 维度 2: 学生个体学情画像 -->
      <StudentPortraitView
        v-else
        :key="`portrait-${selectedStudentId || 'default'}`"
        :portrait="portraitData"
        :student-options="learningData?.students ?? []"
        @switch-student="handleSwitchStudent"
        @back-overall="handleBackOverall"
      />
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import LearningAnalysisHero from '@/components/analytics/LearningAnalysisHero.vue';
import OverallAnalyticsView from '@/components/analytics/OverallAnalyticsView.vue';
import StudentPortraitView from '@/components/analytics/StudentPortraitView.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { getCourseDetail } from '@/api/course/course';

const route = useRoute();
const router = useRouter();

// 优先从路由 query 提取 courseId，未提供时默认课程 102
const initialCourseId = Number(route.query.courseId) || 102;
const { courseOptions, courseId } = useTeacherCourses(initialCourseId);

const range = ref((route.query.range as string) || '7d');
const adviceLoading = ref(false);

const {
  loading,
  usedMockFallback,
  isAggregated,
  learningData,
  portraitData,
  teachingAdvice,
  activeTab,
  selectedStudentId,
  fetchLearning,
  fetchStudentPortrait,
  fetchTeachingAdvice
} = useLearningAnalytics();

// 课程详细元数据 (用于 Banner 展示)
const courseDetailInfo = ref<any>(null);

const currentCourseName = computed(() => {
  if (courseDetailInfo.value?.name) return courseDetailInfo.value.name;
  const match = courseOptions.value.find((c) => c.id === courseId.value);
  return match ? match.name : '当前诊断课程';
});

const currentCourseCode = computed(() => {
  return courseDetailInfo.value?.code || (courseId.value === 102 ? 'AI2026-CS88' : `CRS-${courseId.value}`);
});

const currentTeacherName = computed(() => {
  return courseDetailInfo.value?.teacherName || '张老师';
});

const currentSemester = computed(() => {
  return courseDetailInfo.value?.semester || '2026年秋季学期';
});

// 加载课程元数据
async function loadCourseMeta(cid: number) {
  try {
    const res = await getCourseDetail(cid);
    if (res?.data) {
      courseDetailInfo.value = res.data;
    }
  } catch {
    // 忽略元数据加载错误，使用默认与列表数据兜底
  }
}

// 刷新全量数据
async function reload() {
  await Promise.all([
    loadCourseMeta(courseId.value),
    fetchLearning(courseId.value, range.value)
  ]);

  // 如果路由指定了 studentId 或当前处于 personal tab，加载该学员画像
  const queryStudentId = Number(route.query.studentId);
  if (queryStudentId) {
    activeTab.value = 'personal';
    await fetchStudentPortrait(courseId.value, queryStudentId);
  } else if (activeTab.value === 'personal') {
    // 默认选择第一个学生画像
    const firstStu = learningData.value?.students?.[0];
    const targetId = firstStu ? firstStu.studentId : 3;
    await fetchStudentPortrait(courseId.value, targetId);
  }
}

function syncUrlQuery() {
  const query: Record<string, any> = {
    courseId: courseId.value,
    tab: activeTab.value
  };
  if (range.value !== '7d') {
    query.range = range.value;
  }
  if (activeTab.value === 'personal' && selectedStudentId.value) {
    query.studentId = selectedStudentId.value;
  }
  router.replace({ query });
}

function handleCourseChange(newCourseId: number) {
  courseId.value = newCourseId;
  syncUrlQuery();
  reload();
}

function handleRangeChange(newRange: string) {
  range.value = newRange;
  syncUrlQuery();
  reload();
}

function handleTabChange(tab: 'overall' | 'personal') {
  activeTab.value = tab;
  if (tab === 'personal' && !selectedStudentId.value) {
    const firstStu = learningData.value?.students?.[0];
    const targetId = firstStu ? firstStu.studentId : 3;
    fetchStudentPortrait(courseId.value, targetId);
  }
  syncUrlQuery();
}

async function handleViewStudentPortrait(studentId: number) {
  activeTab.value = 'personal';
  await fetchStudentPortrait(courseId.value, studentId);
  syncUrlQuery();
}

async function handleSwitchStudent(studentId: number) {
  await fetchStudentPortrait(courseId.value, studentId);
  syncUrlQuery();
}

function handleBackOverall() {
  activeTab.value = 'overall';
  syncUrlQuery();
}

async function handleGenerateAdvice() {
  adviceLoading.value = true;
  try {
    await fetchTeachingAdvice({ courseId: courseId.value });
  } finally {
    adviceLoading.value = false;
  }
}

// 监听路由参数变动（支持从外部选课成员列表点击跳转）
watch(
  () => route.query,
  (query) => {
    let shouldReload = false;
    if (query.courseId && Number(query.courseId) !== courseId.value) {
      courseId.value = Number(query.courseId);
      shouldReload = true;
    }
    if (query.tab && (query.tab === 'overall' || query.tab === 'personal')) {
      activeTab.value = query.tab;
    }
    if (query.studentId && Number(query.studentId) !== selectedStudentId.value) {
      activeTab.value = 'personal';
      fetchStudentPortrait(courseId.value, Number(query.studentId));
    } else if (shouldReload) {
      reload();
    }
  }
);

onMounted(() => {
  if (route.query.tab === 'personal' || route.query.studentId) {
    activeTab.value = 'personal';
  }
  reload();
});
</script>

<style scoped lang="scss">
.learning-analysis-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.mock-fallback-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 9999px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  font-size: 12px;
  color: #64748B;

  .pill-svg {
    width: 14px;
    height: 14px;
    color: #94A3B8;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
