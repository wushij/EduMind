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
      :personal-tab-enabled="personalTabEnabled"
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
        @clear-advice="handleClearAdvice"
        @stop-advice="handleStopAdvice"
      />

      <!-- 维度 2: 学生个体学情画像 -->
      <div v-else-if="!personalTabEnabled" key="personal-empty" class="personal-empty-panel">
        <div class="personal-empty-card">
          <h3>暂无选课学员</h3>
          <p>当前课程还没有学生加入，无法生成个体学情画像。请先在「选课班级成员」中添加学员，或让学生通过课程代号选课。</p>
          <button type="button" class="capsule-btn capsule-btn--primary" @click="handleBackOverall">
            返回班级整体分析
          </button>
        </div>
      </div>
      <StudentPortraitView
        v-else
        :key="`portrait-${selectedStudentId || 'default'}`"
        :portrait="portraitData"
        :student-options="enrolledStudents"
        :advice-loading="adviceLoading"
        @switch-student="handleSwitchStudent"
        @back-overall="handleBackOverall"
        @generate-advice="handleGenerateAdvice"
        @clear-advice="handleClearPersonalAdvice"
        @stop-advice="handleStopAdvice"
        @open-diagnosis-drawer="aiDrawerVisible = true"
      />
    </transition>

    <!-- AI 学情诊断决策抽屉 (长圆药丸美学 + 靶向干预) -->
    <AiDiagnosisDrawer
      v-model="aiDrawerVisible"
      :advice="teachingAdvice"
      :mode="activeTab"
      :target-student-name="portraitData?.studentInfo.realName"
      :weak-points="activeWeakPoints"
      @dispatch-practice="handleDispatchPractice"
      @clear-advice="handleClearCurrentAdvice"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import LearningAnalysisHero from '@/components/analytics/LearningAnalysisHero.vue';
import OverallAnalyticsView from '@/components/analytics/OverallAnalyticsView.vue';
import StudentPortraitView from '@/components/analytics/StudentPortraitView.vue';
import AiDiagnosisDrawer from '@/components/analytics/AiDiagnosisDrawer.vue';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { getCourseDetail } from '@/api/course/course';

const route = useRoute();
const router = useRouter();

// 优先从路由 query 提取 courseId，未提供时默认课程 102
const initialCourseId = Number(route.query.courseId) || 102;
const { courseOptions, courseId } = useTeacherCourses(initialCourseId);

const range = ref((route.query.range as string) || '7d');
const aiDrawerVisible = ref(false);

const {
  loading,
  adviceLoading,
  usedMockFallback,
  isAggregated,
  learningData,
  portraitData,
  teachingAdvice,
  activeTab,
  selectedStudentId,
  fetchLearning,
  fetchStudentPortrait,
  fetchTeachingAdvice,
  clearTeachingAdvice,
  stopTeachingAdvice,
  loadStoredAdvice
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

const enrolledStudents = computed(() => learningData.value?.students ?? []);

const personalTabEnabled = computed(() => enrolledStudents.value.length > 0);

function resolvePortraitStudentId(): number | null {
  const fromQuery = Number(route.query.studentId);
  if (fromQuery && enrolledStudents.value.some((s) => s.studentId === fromQuery)) {
    return fromQuery;
  }
  if (selectedStudentId.value && enrolledStudents.value.some((s) => s.studentId === selectedStudentId.value)) {
    return selectedStudentId.value;
  }
  return enrolledStudents.value[0]?.studentId ?? null;
}

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
  if (queryStudentId && personalTabEnabled.value && enrolledStudents.value.some((s) => s.studentId === queryStudentId)) {
    activeTab.value = 'personal';
    await fetchStudentPortrait(courseId.value, queryStudentId);
  } else if (activeTab.value === 'personal') {
    if (!personalTabEnabled.value) {
      activeTab.value = 'overall';
      portraitData.value = null;
      selectedStudentId.value = null;
      loadStoredAdvice(courseId.value, null);
    } else {
      const targetId = resolvePortraitStudentId();
      if (targetId) {
        await fetchStudentPortrait(courseId.value, targetId);
      }
    }
  } else {
    loadStoredAdvice(courseId.value, null);
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
  if (tab === 'personal' && !personalTabEnabled.value) {
    ElMessage.warning('当前课程暂无选课学员，无法查看个体学情画像');
    return;
  }
  activeTab.value = tab;
  if (tab === 'personal') {
    const targetId = resolvePortraitStudentId();
    if (targetId) {
      fetchStudentPortrait(courseId.value, targetId);
    }
  } else {
    loadStoredAdvice(courseId.value, null);
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
  loadStoredAdvice(courseId.value, null);
  syncUrlQuery();
}

const activeWeakPoints = computed(() => {
  if (activeTab.value === 'personal') {
    return portraitData.value?.weakPoints?.map((w) => w.title) ?? [];
  }
  return [];
});

async function handleGenerateAdvice() {
  const isPersonal = activeTab.value === 'personal';
  const targetStudentId = isPersonal
    ? (selectedStudentId.value || portraitData.value?.studentInfo?.studentId)
    : undefined;

  try {
    const advice = await fetchTeachingAdvice({
      courseId: courseId.value,
      studentId: targetStudentId
    });

    if (!advice) {
      // 手动中止/暂停
      return;
    }

    if (isPersonal && advice.summary && portraitData.value) {
      portraitData.value.aiDiagnosis = advice.summary;
    }

    ElMessage.success({
      message: isPersonal
        ? `已成功生成针对「${portraitData.value?.studentInfo?.realName || '该学员'}」的精准学情诊断！`
        : '已成功生成全班教学诊断策略决策报告！',
      duration: 3000
    });

    aiDrawerVisible.value = true;
  } catch {
    ElMessage.error('生成学情诊断建议失败，请稍后重试');
  }
}

function handleStopAdvice() {
  stopTeachingAdvice();
  ElMessage.info('已手动停止本次 AI 推演');
}

function handleClearAdvice() {
  clearTeachingAdvice(courseId.value, null);
  ElMessage.success('已清空当前全班 AI 教学策略建议');
}

function handleClearPersonalAdvice() {
  if (portraitData.value) {
    portraitData.value.aiDiagnosis = '';
  }
  clearTeachingAdvice(courseId.value, selectedStudentId.value);
  ElMessage.success('已清空当前学员的 AI 诊断建议');
}

function handleClearCurrentAdvice() {
  if (activeTab.value === 'personal') {
    handleClearPersonalAdvice();
  } else {
    handleClearAdvice();
  }
}

function handleDispatchPractice() {
  ElMessage.success(`已为「${portraitData.value?.studentInfo?.realName || '学员'}」定向下发 5 道考点自适应强化题！`);
  aiDrawerVisible.value = false;
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
      const sid = Number(query.studentId);
      if (personalTabEnabled.value && enrolledStudents.value.some((s) => s.studentId === sid)) {
        activeTab.value = 'personal';
        fetchStudentPortrait(courseId.value, sid);
      } else {
        activeTab.value = 'overall';
      }
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

.personal-empty-panel {
  display: flex;
  justify-content: center;
  padding: 48px 16px 64px;
}

.personal-empty-card {
  max-width: 520px;
  padding: 32px 28px;
  text-align: center;
  background: #fff;
  border-radius: 20px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);

  h3 {
    margin: 0 0 12px;
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0 0 24px;
    font-size: 14px;
    line-height: 1.7;
    color: var(--el-text-color-secondary);
  }
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
