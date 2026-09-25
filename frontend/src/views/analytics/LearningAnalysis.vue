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
      :can-view-overall="!isStudentViewer"
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
        key="student-portrait"
        :portrait="portraitData"
        :student-options="portraitStudentOptions"
        :variant="isStudentViewer ? 'student' : 'teacher'"
        :advice-loading="adviceLoading"
        :loading="loading"
        @switch-student="handleSwitchStudent"
        @back-overall="handleBackOverall"
        @generate-advice="handleGenerateAdvice"
        @clear-advice="handleClearPersonalAdvice"
        @stop-advice="handleStopAdvice"
        @open-diagnosis-drawer="handleOpenDiagnosisDrawer"
      />
    </transition>

    <!-- AI 智教认知推演引擎弹窗 (与其他模块完全对齐：640px 弹窗 + 罗盘脉冲雷达 + 0.1s 秒表实时递增 + 流水线推进 + 随时中止) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      :title="activeTab === 'personal' ? 'AI 个人学情精准诊断推演引擎' : 'AI 全班教学质效评估推演引擎'"
      width="640px"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="true"
      append-to-body
      @close="handleStopAdvice"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.analyticsTeachingAdvice"
        show-footer-actions
        abort-label="中止推演"
        @abort="handleStopAdvice"
      />
    </el-dialog>

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

    <!-- 教师点击个人画像 Tab 未选学生时的选择弹窗 -->
    <el-dialog
      v-model="showStudentSelectModal"
      title="选择诊断学员"
      width="560px"
      append-to-body
      class="student-picker-dialog"
      :before-close="handleCancelStudentPicker"
    >
      <div class="student-picker-header">
        <p class="picker-tip">请选择当前班级中的一位学员，查看其个体能力雷达与薄弱诊断画像：</p>
        <el-input
          v-model="pickerSearchKeyword"
          placeholder="搜索学员姓名、学号..."
          clearable
          :prefix-icon="Search"
          class="picker-search-input"
        />
      </div>

      <div class="picker-student-list">
        <div
          v-for="s in filteredPickerStudents"
          :key="s.studentId"
          class="picker-student-card"
          @click="handleSelectStudentFromModal(Number(s.studentId))"
        >
          <el-avatar :size="42" :src="s.avatar" class="picker-avatar">
            {{ s.realName?.slice(0, 1) || '学' }}
          </el-avatar>
          <div class="picker-student-meta">
            <div class="picker-name-row">
              <span class="picker-name">{{ s.realName }}</span>
              <span class="picker-sno">{{ s.studentNo || s.username }}</span>
            </div>
            <div class="picker-score-row">
              <span class="picker-stat">掌握度: {{ (s.masteryScore ?? 0).toFixed(1) }}%</span>
              <span class="picker-divider">·</span>
              <span class="picker-stat">在线时长: {{ s.studyMinutes ?? 0 }}分钟</span>
            </div>
          </div>
          <button type="button" class="capsule-btn capsule-btn--primary picker-choose-btn">
            查看画像
          </button>
        </div>

        <div v-if="filteredPickerStudents.length === 0" class="picker-empty">
          <p>暂无可选的班级学员</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import LearningAnalysisHero from '@/components/analytics/LearningAnalysisHero.vue';
import OverallAnalyticsView from '@/components/analytics/OverallAnalyticsView.vue';
import StudentPortraitView from '@/components/analytics/StudentPortraitView.vue';
import AiDiagnosisDrawer from '@/components/analytics/AiDiagnosisDrawer.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { getCourseDetail } from '@/api/course/course';
import { useAuthStore } from '@/stores/auth/auth';
import { RoleEnum } from '@/constants/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

/**
 * 学生视角判定：仅具备学生角色（无教师/管理员）时，
 * 页面必须锁定为「本人学情画像」，不得展示班级整体学情分析。
 * 后端对班级接口同样做了角色拦截，这里是前端的第一道防线。
 */
const isStudentViewer = computed(() => !authStore.hasAnyRole([RoleEnum.ADMIN, RoleEnum.TEACHER]));
const viewerId = computed(() => authStore.currentUser?.id ?? null);

// 课程来源优先级：URL query 显式指定 → 上次访问的课程 → 可用课程列表首项。
// 不再硬编码默认课程 id：写死某门课程会让任何教师从侧栏进入时都先看到同一门课，
// 与「我当前在教哪门课」的实际上下文无关。
const initialCourseId = Number(route.query.courseId) || undefined;
const { courseOptions, courseId, courseIdCorrected } = useTeacherCourses(initialCourseId);

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

// 课程代号 / 主讲教师 / 开课学期一律以课程详情接口为准。
// 拿不到时返回空串让 Hero 自行隐藏或占位，不再用 'AI2026-CS88'、'张老师'、'2026年秋季学期'
// 这类 mock 值冒充真实课程信息（会让人误以为诊断的是那门写死的课程）。
const currentCourseCode = computed(() => courseDetailInfo.value?.code || '');

const currentTeacherName = computed(() => courseDetailInfo.value?.teacherName || '');

const currentSemester = computed(() => courseDetailInfo.value?.semester || '');

const enrolledStudents = computed(() => learningData.value?.students ?? []);

// 学生视角下不依赖班级名册：只要登录用户存在即可查看本人画像
const personalTabEnabled = computed(() =>
  isStudentViewer.value ? viewerId.value != null : enrolledStudents.value.length > 0
);

/** 个体画像的可切换学员列表：学生视角不暴露班级名册，仅保留本人上下文 */
const portraitStudentOptions = computed(() => (isStudentViewer.value ? [] : enrolledStudents.value));

const showStudentSelectModal = ref(false);
const pickerSearchKeyword = ref('');

function isRealStudent(s: any): boolean {
  const name = s.realName || '';
  const u = s.username || '';
  return !name.includes('管理员') && !name.toLowerCase().includes('admin') && u.toLowerCase() !== 'admin';
}

const filteredPickerStudents = computed(() => {
  const kw = pickerSearchKeyword.value.trim().toLowerCase();
  const list = enrolledStudents.value.filter(isRealStudent);
  const candidates = list.length > 0 ? list : enrolledStudents.value;
  if (!kw) return candidates;
  return candidates.filter(
    (s) =>
      (s.realName && s.realName.toLowerCase().includes(kw)) ||
      (s.studentNo && s.studentNo.toLowerCase().includes(kw)) ||
      (s.username && s.username.toLowerCase().includes(kw))
  );
});

function handleCancelStudentPicker() {
  showStudentSelectModal.value = false;
  activeTab.value = 'overall';
  syncUrlQuery();
}

async function handleSelectStudentFromModal(studentId: number) {
  showStudentSelectModal.value = false;
  selectedStudentId.value = studentId;
  activeTab.value = 'personal';
  syncUrlQuery();
  await fetchStudentPortrait(courseId.value, studentId, range.value);
}

function resolvePortraitStudentId(): number | null {
  const fromQuery = Number(route.query.studentId);
  // URL 已明确指定学员时，绝不退化成「名册第一个」：
  // 否则从成员页点「杨同学」的学情画像，一旦该生不在当前课程名册里就会静默显示
  // 名册第一人（如超级管理员）的画像，让人误以为看的是杨同学的数据。
  if (fromQuery) {
    return enrolledStudents.value.some((s) => Number(s.studentId) === Number(fromQuery)) ? fromQuery : null;
  }
  if (selectedStudentId.value && enrolledStudents.value.some((s) => Number(s.studentId) === Number(selectedStudentId.value))) {
    return Number(selectedStudentId.value);
  }
  const realStudent = enrolledStudents.value.find(isRealStudent);
  if (realStudent?.studentId != null) {
    return Number(realStudent.studentId);
  }
  return enrolledStudents.value[0]?.studentId != null ? Number(enrolledStudents.value[0].studentId) : null;
}

// 加载课程元数据
async function loadCourseMeta(cid: number) {
  try {
    const res = await getCourseDetail(cid);
    courseDetailInfo.value = res?.data ?? null;
  } catch {
    // 加载失败（无权访问该课程 / 课程已删除）必须清空，否则会继续沿用上一门课程的
    // 标题、课程代号、教师与学期，页面就会变成「标题是 A 课、返回按钮跳 B 课」。
    // 清空后由 courseOptions 兜底渲染，至少保证显示的是当前 courseId 对应的课程。
    courseDetailInfo.value = null;
  }
}

// 刷新全量数据
async function reload() {
  // 学生视角：不请求班级整体数据（后端同样会拒绝），只加载本人画像
  if (isStudentViewer.value) {
    activeTab.value = 'personal';
    await loadCourseMeta(courseId.value);
    if (viewerId.value != null) {
      await fetchStudentPortrait(courseId.value, viewerId.value, range.value);
    }
    return;
  }

  await Promise.all([
    loadCourseMeta(courseId.value),
    fetchLearning(courseId.value, range.value)
  ]);

  // 如果路由指定了 studentId 或当前处于 personal tab，加载该学员画像
  const queryStudentId = Number(route.query.studentId);
  if (queryStudentId && personalTabEnabled.value && enrolledStudents.value.some((s) => Number(s.studentId) === Number(queryStudentId))) {
    activeTab.value = 'personal';
    await fetchStudentPortrait(courseId.value, queryStudentId, range.value);
  } else if (activeTab.value === 'personal') {
    if (!personalTabEnabled.value) {
      activeTab.value = 'overall';
      portraitData.value = null;
      selectedStudentId.value = null;
      loadStoredAdvice(courseId.value, null);
    } else {
      const targetId = resolvePortraitStudentId();
      if (targetId) {
        await fetchStudentPortrait(courseId.value, targetId, range.value);
      } else if (queryStudentId) {
        // 路由明确指定了学员，但该学员不在本课名册：退回班级整体视图并说明原因，
        // 不再静默展示名册里其他人的画像
        activeTab.value = 'overall';
        portraitData.value = null;
        selectedStudentId.value = null;
        ElMessage.warning('该学员不在当前课程名册中，已返回班级整体分析');
        loadStoredAdvice(courseId.value, null);
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

/**
 * 目标课程不可访问、被 useTeacherCourses 自动纠正为可用课程后必须重载。
 * 否则页面会停留在「用无权课程请求失败」的旧状态：标题来自兜底数据、
 * 而内部 courseId 已是另一门课，点「返回课程空间」就会跳回那门无权课程。
 */
watch(courseIdCorrected, (corrected) => {
  if (!corrected) return;
  const match = courseOptions.value.find((c) => Number(c.id) === Number(courseId.value));
  const queryCourseId = Number(route.query.courseId);
  if (queryCourseId && queryCourseId !== Number(courseId.value)) {
    ElMessage.warning(`无权访问原课程，已切换到「${match?.name ?? '可用课程'}」`);
  }
  syncUrlQuery();
  reload();
});

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
  // 学生账号不得进入班级整体分析
  if (tab === 'overall' && isStudentViewer.value) {
    ElMessage.warning('学生账号仅可查看本人学情画像');
    return;
  }
  if (tab === 'personal' && !personalTabEnabled.value) {
    ElMessage.warning('当前课程暂无选课学员，无法查看个体学情画像');
    return;
  }
  if (tab === 'personal') {
    // 若当前没有已选定的学员，弹出学员选择窗口供老师明确点选
    if (!selectedStudentId.value && !Number(route.query.studentId) && enrolledStudents.value.length > 0) {
      showStudentSelectModal.value = true;
      pickerSearchKeyword.value = '';
      return;
    }
    activeTab.value = 'personal';
    const targetId = resolvePortraitStudentId();
    if (targetId) {
      selectedStudentId.value = targetId;
      syncUrlQuery();
      fetchStudentPortrait(courseId.value, targetId, range.value);
    }
  } else {
    activeTab.value = 'overall';
    loadStoredAdvice(courseId.value, null);
    syncUrlQuery();
  }
}

async function handleViewStudentPortrait(studentId: number) {
  selectedStudentId.value = studentId;
  activeTab.value = 'personal';
  syncUrlQuery();
  await fetchStudentPortrait(courseId.value, studentId, range.value);
}

async function handleSwitchStudent(studentId: number) {
  selectedStudentId.value = studentId;
  syncUrlQuery();
  await fetchStudentPortrait(courseId.value, studentId, range.value);
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

const aiThinkingModalVisible = ref(false);
let isThinkingAborted = false;

async function handleGenerateAdvice() {
  // AI 学情诊断建议属于教师的教学干预动作，学生端仅提供只读画像
  if (isStudentViewer.value) {
    ElMessage.warning('AI 学情诊断建议由教师生成，学生端仅可查看本人画像');
    return;
  }
  const isPersonal = activeTab.value === 'personal';
  const targetStudentId = isPersonal
    ? (selectedStudentId.value || portraitData.value?.studentInfo?.studentId)
    : undefined;

  isThinkingAborted = false;
  aiThinkingModalVisible.value = true;
  try {
    const fetchPromise = fetchTeachingAdvice({
      courseId: courseId.value,
      studentId: targetStudentId
    });
    // 保持至少 1.8 秒沉浸式推演流水线体验，确保秒表与罗盘波纹正常运转
    const [advice] = await Promise.all([
      fetchPromise,
      new Promise((resolve) => setTimeout(resolve, 1800))
    ]);

    if (isThinkingAborted || !advice) {
      return;
    }

    aiThinkingModalVisible.value = false;

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
    if (!isThinkingAborted) {
      aiThinkingModalVisible.value = false;
      ElMessage.error('生成学情诊断建议失败，请稍后重试');
    }
  }
}

function handleStopAdvice() {
  isThinkingAborted = true;
  stopTeachingAdvice();
  aiThinkingModalVisible.value = false;
  ElMessage.info('已中止本次 AI 教学推演');
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

/**
 * 打开 AI 诊断抽屉。
 * 没有真实诊断数据时不打开空抽屉，避免出现「看起来有结论、其实没有」的观感。
 */
function handleOpenDiagnosisDrawer() {
  if (isStudentViewer.value) {
    ElMessage.warning('AI 学情诊断由教师生成，学生端仅可查看本人画像数据');
    return;
  }
  if (!teachingAdvice.value) {
    ElMessage.info('请先点击右上角「生成个人学情诊断」，生成后再查看诊断处方');
    return;
  }
  aiDrawerVisible.value = true;
}

function handleDispatchPractice() {
  aiDrawerVisible.value = false;
  const query: Record<string, string> = {
    courseId: String(courseId.value),
    autoCreate: 'true'
  };
  const targetStudentId = portraitData.value?.studentInfo?.studentId;
  if (activeTab.value === 'personal' && targetStudentId) {
    query.studentId = String(targetStudentId);
  }
  const weakPoint = learningData.value?.courseWeakPoints?.[0];
  if (weakPoint?.title) {
    query.knowledgePointTitle = weakPoint.title;
    if (weakPoint.knowledgePointId) {
      query.knowledgePointId = String(weakPoint.knowledgePointId);
    }
  }
  router.push({ path: '/analytics/interventions', query });
}

// 监听路由参数变动（支持从外部选课成员列表点击跳转）
watch(
  () => route.query,
  (query) => {
    // 学生视角：无论 URL 传入什么 tab，一律锁定为本人画像
    if (isStudentViewer.value) {
      activeTab.value = 'personal';
      if (query.courseId && Number(query.courseId) !== courseId.value) {
        courseId.value = Number(query.courseId);
        reload();
      }
      return;
    }
    let shouldReload = false;
    if (query.courseId && Number(query.courseId) !== courseId.value) {
      courseId.value = Number(query.courseId);
      shouldReload = true;
    }
    if (query.tab && (query.tab === 'overall' || query.tab === 'personal')) {
      activeTab.value = query.tab;
    }
    if (query.studentId && Number(query.studentId) !== Number(selectedStudentId.value)) {
      const sid = Number(query.studentId);
      if (personalTabEnabled.value && enrolledStudents.value.some((s) => Number(s.studentId) === sid)) {
        selectedStudentId.value = sid;
        activeTab.value = 'personal';
        fetchStudentPortrait(courseId.value, sid, range.value);
      } else {
        activeTab.value = 'overall';
      }
    } else if (shouldReload) {
      reload();
    }
  }
);

onMounted(() => {
  if (isStudentViewer.value) {
    activeTab.value = 'personal';
  } else if (route.query.tab === 'personal' || route.query.studentId) {
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

.student-picker-header {
  margin-bottom: 16px;

  .picker-tip {
    margin: 0 0 12px;
    font-size: 13px;
    color: #64748B;
    line-height: 1.5;
  }

  .picker-search-input {
    width: 100%;
  }
}

.picker-student-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}

.picker-student-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #EFF6FF;
    border-color: #BFDBFE;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);

    .picker-choose-btn {
      opacity: 1;
    }
  }

  .picker-avatar {
    flex-shrink: 0;
    background: #1677FF;
    color: #fff;
    font-weight: 600;
  }

  .picker-student-meta {
    flex: 1;
    min-width: 0;

    .picker-name-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      .picker-name {
        font-size: 14px;
        font-weight: 600;
        color: #0F172A;
      }

      .picker-sno {
        font-size: 12px;
        color: #64748B;
        background: #E2E8F0;
        padding: 1px 6px;
        border-radius: 4px;
      }
    }

    .picker-score-row {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #64748B;

      .picker-divider {
        color: #CBD5E1;
      }
    }
  }

  .picker-choose-btn {
    flex-shrink: 0;
    font-size: 12px;
    padding: 6px 14px;
    opacity: 0.85;
  }
}

.picker-empty {
  padding: 32px 0;
  text-align: center;
  color: #94A3B8;
  font-size: 13px;
}
</style>
