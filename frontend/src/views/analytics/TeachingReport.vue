<template>
  <div class="teaching-report-container" v-loading="loading">
    <!-- 1. 顶部科技毛玻璃 Hero 横幅 (对齐学情概览与学情分析视觉) -->
    <TeachingReportHero
      :course-id="courseId"
      :course-options="courseOptions"
      :course-name="courseName"
      :course-code="courseCode"
      :teacher-name="teacherName"
      :student-count="studentCount"
      :syllabus-progress="syllabusProgress"
      :total-chapters="totalChapters"
      :has-real-data="hasRealData"
      :range="currentRange"
      :loading="loading"
      :courses-loading="coursesLoading"
      :advice-loading="adviceLoading"
      :last-updated-time="lastUpdatedTime"
      @change-course="handleCourseChange"
      @change-range="handleRangeChange"
      @refresh="loadReport"
      @export="handleExportReport"
      @open-ai-advice="handleOpenAiAdvice"
    />

    <!-- 2. 四大核心 KPI 仪表盘 -->
    <TeachingReportKpi
      :pass-rate="passRate"
      :mastery-rate="masteryRate"
      :mastery-estimated="masteryEstimated"
      :ai-call-count="aiCallCount"
      :saved-hours="savedHours"
      :saved-hours-estimated="savedHoursEstimated"
      :graded-count="reportData?.gradedCount ?? 0"
    />

    <!-- 3. 主体分栏：左侧掌握度热力榜与答疑负荷，右侧错因聚类与策略建议 -->
    <div class="analytics-main-split">
      <TeachingReportCharts
        :knowledge-mastery-list="knowledgeMasteryList"
        :weekly-activity="weeklyActivity"
        :on-quick-quiz="handleQuickQuiz"
        :on-view-question="handleViewQuestion"
      />

      <TeachingAdvicePanel
        ref="advicePanelRef"
        :router="router"
        :course-id="courseId"
        :report-data="reportData"
        :teaching-advice="teachingAdvice"
        :top-weak-point-names="topWeakPointNames"
        :error-categories="errorCategories"
      />
    </div>

    <!-- 4. 考点题目与错因下钻抽屉 -->
    <TeachingQuestionDetailDrawer
      v-model="questionDrawerVisible"
      :question="selectedQuestion"
      @quick-quiz="handleQuickQuiz"
    />

    <!-- 5. AI 智教认知推演引擎弹窗 (与其他模块完全对齐：640px 弹窗 + 罗盘脉冲雷达 + 0.1s 秒表实时递增 + 流水线推进) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      title="AI 全班教学质效评估与学情深度推演引擎"
      width="640px"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="true"
      append-to-body
      @close="handleStopAiAdvice"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.analyticsTeachingAdvice"
        show-footer-actions
        abort-label="中止推演"
        @abort="handleStopAiAdvice"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import TeachingReportHero from '@/components/analytics/TeachingReportHero.vue';
import TeachingReportKpi from '@/components/analytics/TeachingReportKpi.vue';
import TeachingReportCharts from '@/components/analytics/TeachingReportCharts.vue';
import TeachingAdvicePanel from '@/components/analytics/TeachingAdvicePanel.vue';
import TeachingQuestionDetailDrawer from '@/components/analytics/TeachingQuestionDetailDrawer.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { useTeachingReport } from '@/composables/analytics/useTeachingReport';

const {
  router,
  loading,
  coursesLoading,
  courseOptions,
  courseId,
  courseName,
  courseCode,
  teacherName,
  studentCount,
  syllabusProgress,
  totalChapters,
  currentRange,
  reportData,
  passRate,
  masteryRate,
  masteryEstimated,
  aiCallCount,
  savedHours,
  savedHoursEstimated,
  hasRealData,
  lastUpdatedTime,
  topWeakPointNames,
  knowledgeMasteryList,
  weeklyActivity,
  errorCategories,
  questionDrawerVisible,
  selectedQuestion,
  aiThinkingModalVisible,
  adviceLoading,
  teachingAdvice,
  adviceGeneratedTick,
  loadReport,
  handleCourseChange,
  handleRangeChange,
  handleOpenAiAdvice,
  handleStopAiAdvice,
  handleViewQuestion,
  handleExportReport,
  handleQuickQuiz
} = useTeachingReport();

/** 建议面板引用：诊断生成完成后把用户视线带到右栏结果卡片 */
const advicePanelRef = ref<InstanceType<typeof TeachingAdvicePanel> | null>(null);

watch(adviceGeneratedTick, () => {
  advicePanelRef.value?.focusAdvice();
});
</script>

<style scoped lang="scss">
.teaching-report-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .analytics-main-split {
    display: grid;
    grid-template-columns: 58% 42%;
    gap: 20px;
  }
}

@media (max-width: 1280px) {
  .analytics-main-split {
    grid-template-columns: 1fr !important;
  }
}
</style>

