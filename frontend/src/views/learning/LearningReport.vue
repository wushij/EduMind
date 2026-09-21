<template>
  <div class="learning-report-page" v-loading="loading">
    <LearningSubpageHero
      title="个人学情深度分析与诊断报告"
      subtitle="多维评估个人知识掌握曲线、作业作答质量与认知提升轨迹，智能输出个性化学期突破建议"
      variant="analytics"
    >
      <template #toolbar>
        <el-select
          v-if="courseOptions.length"
          v-model="courseId"
          placeholder="选择课程"
          style="width: 220px"
          @change="onFiltersChange"
        >
          <el-option
            v-for="c in courseOptions"
            :key="c.id"
            :label="c.name || (c as any).title || ('课程 #' + c.id)"
            :value="c.id"
          />
        </el-select>
        <el-radio-group v-model="timeRange" @change="onFiltersChange">
          <el-radio-button label="7d">近 7 天</el-radio-button>
          <el-radio-button label="30d">近 30 天</el-radio-button>
          <el-radio-button label="term">本学期</el-radio-button>
        </el-radio-group>
        <button type="button" class="capsule-btn capsule-btn--default" @click="goWrongBook">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
          <span>错题本</span>
        </button>
        <button type="button" class="capsule-btn capsule-btn--ai" @click="goPractice">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          <span>AI 练习</span>
        </button>
        <button type="button" class="capsule-btn capsule-btn--default" @click="router.push('/learning/path')">
          <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"></polygon>
          </svg>
          <span>学习路径</span>
        </button>
      </template>
    </LearningSubpageHero>

    <div v-if="notEnrolled" class="empty-enrolled-panel">
      <el-empty description="您尚未加入任何课程，或当前账号无法查看个人学情报告">
        <button type="button" class="capsule-btn capsule-btn--primary" @click="router.push('/course')">
          前往课程中心选课
        </button>
      </el-empty>
    </div>

    <StudentPortraitView
      v-else-if="portrait"
      variant="student"
      :portrait="displayPortrait"
      :personal-trends="reportTrends"
      :report-code="reportCode"
      :advice-loading="adviceLoading"
      @generate-advice="handleGenerateAdvice"
      @clear-advice="handleClearAdvice"
      @stop-advice="stopAdvice"
      @open-diagnosis-drawer="aiDrawerVisible = true"
      @go-practice="goPractice"
      @go-wrong-book="goWrongBook"
    />

    <AiDiagnosisDrawer
      v-model="aiDrawerVisible"
      :advice="teachingAdvice"
      mode="personal"
      :target-student-name="portrait?.studentInfo?.realName"
      :weak-points="portrait?.weakPoints?.map(wp => wp.title) ?? []"
      @clear-advice="handleClearAdvice"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import LearningSubpageHero from '@/components/learning/LearningSubpageHero.vue';
import StudentPortraitView from '@/components/analytics/StudentPortraitView.vue';
import AiDiagnosisDrawer from '@/components/analytics/AiDiagnosisDrawer.vue';
import { useLearningReport } from '@/composables/learning/useLearningReport';
import { useStudentLearningDiagnosis } from '@/composables/learning/useStudentLearningDiagnosis';

const router = useRouter();
const {
  loading,
  courseId,
  courseOptions,
  timeRange,
  portrait,
  reportTrends,
  notEnrolled,
  reportCode,
  loadReport,
  init
} = useLearningReport();

const {
  adviceLoading,
  teachingAdvice,
  generateAdvice,
  clearAdvice,
  stopAdvice,
  loadStoredAdvice
} = useStudentLearningDiagnosis();

const aiDrawerVisible = ref(false);

const displayPortrait = computed(() => {
  if (!portrait.value) {
    return null;
  }
  if (teachingAdvice.value?.summary && !portrait.value.aiDiagnosis) {
    return {
      ...portrait.value,
      aiDiagnosis: teachingAdvice.value.summary
    };
  }
  return portrait.value;
});

async function onFiltersChange() {
  await loadReport();
  syncAdvice();
}

function syncAdvice() {
  if (!courseId.value || !portrait.value?.studentInfo?.studentId) {
    return;
  }
  const sid = portrait.value.studentInfo.studentId;
  loadStoredAdvice(courseId.value, sid);
}

async function handleGenerateAdvice() {
  if (!courseId.value) return;
  const sid = portrait.value?.studentInfo?.studentId;
  const advice = await generateAdvice(courseId.value, sid);
  if (advice) {
    ElMessage.success('AI 学情诊断建议已生成');
  }
}

function handleClearAdvice() {
  ElMessageBox.confirm('确定清空当前 AI 诊断建议吗？', '清空建议确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      if (courseId.value) {
        clearAdvice(courseId.value, portrait.value?.studentInfo?.studentId);
      }
    })
    .catch(() => {});
}

function goPractice() {
  router.push({ path: '/learning/practice', query: { courseId: String(courseId.value) } });
}

function goWrongBook() {
  router.push({ path: '/learning/wrong-questions', query: { courseId: String(courseId.value) } });
}

watch(portrait, () => syncAdvice());

onMounted(async () => {
  await init();
  syncAdvice();
});
</script>

<style scoped lang="scss">
.learning-report-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 48px;
}

.empty-enrolled-panel {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 48px 24px;
}

:deep(.capsule-btn) {
  margin-left: 0;
}
</style>
