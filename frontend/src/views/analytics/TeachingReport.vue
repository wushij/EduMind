<template>
  <div class="teaching-report-container">
    <PageHeroBanner
      title="教学分析"
      subtitle="汇总课程学情、作业测验与 AI 助教使用数据，辅助教学决策"
      background-variant="default"
      :show-illustration="false"
    >
      <template #actions>
        <div class="report-actions-dock">
          <div class="semester-pill-chip">
            <el-icon class="pill-prefix-icon"><Calendar /></el-icon>
            <span>{{ evaluationPeriod }}</span>
          </div>

          <button
            type="button"
            class="capsule-btn capsule-btn--primary"
            @click="handleExportReport"
          >
            <el-icon class="btn-prefix-icon"><Download /></el-icon>
            <span>导出教学质量评估周报</span>
          </button>
        </div>
      </template>
    </PageHeroBanner>

    <TeachingReportKpi
      :pass-rate="passRate"
      :mastery-rate="masteryRate"
      :ai-call-count="aiCallCount"
      :saved-hours="savedHours"
    />

    <div class="analytics-main-split">
      <TeachingReportCharts
        :knowledge-mastery-list="knowledgeMasteryList"
        :weekly-activity="weeklyActivity"
        :on-quick-quiz="handleQuickQuiz"
      />

      <TeachingAdvicePanel
        :router="router"
        :course-id="courseId"
        :report-data="reportData"
        :top-weak-point-names="topWeakPointNames"
        :error-categories="errorCategories"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Calendar, Download } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import TeachingReportKpi from '@/components/analytics/TeachingReportKpi.vue';
import TeachingReportCharts from '@/components/analytics/TeachingReportCharts.vue';
import TeachingAdvicePanel from '@/components/analytics/TeachingAdvicePanel.vue';
import { useTeachingReport } from '@/composables/analytics/useTeachingReport';

const {
  router,
  courseId,
  reportData,
  passRate,
  masteryRate,
  aiCallCount,
  savedHours,
  evaluationPeriod,
  topWeakPointNames,
  knowledgeMasteryList,
  weeklyActivity,
  errorCategories,
  handleExportReport,
  handleQuickQuiz
} = useTeachingReport();
</script>

<style scoped lang="scss">
.teaching-report-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  .report-actions-dock {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .semester-pill-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 16px;
      border-radius: 9999px;
      background: rgba(255, 255, 255, 0.88);
      border: 1px solid rgba(226, 232, 240, 0.8);
      color: #334155;
      font-size: 13px;
      font-weight: 600;

      .pill-prefix-icon {
        font-size: 14px;
        color: #1677FF;
      }
    }

    .capsule-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 20px;
      border-radius: 9999px;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;

      .btn-prefix-icon {
        font-size: 15px;
      }

      &--primary {
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.28);

        &:hover {
          background: #4096FF;
          transform: translateY(-2px);
        }
      }
    }
  }

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
