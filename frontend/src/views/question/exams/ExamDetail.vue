<template>
  <div class="exam-detail-container">
    <ExamDetailHeroSection
      :router="router"
      :loading="loading"
      :exam-data="examData"
      :get-status-label="getStatusLabel"
      :get-status-tag-type="getStatusTagType"
      @publish="handlePublishAsAssignment"
      @export="handleExportPaper"
    />

    <div class="main-content-layout">
      <ExamDetailPaperDisplay
        v-model:view-mode="viewMode"
        :exam-data="examData"
        :grouped-sections="groupedSections"
        :total-questions-count="totalQuestionsCount"
        :get-chinese-number="getChineseNumber"
        @print="printPaper"
      />
      <ExamDetailStatsSidebar
        :exam-data="examData"
        :grouped-sections="groupedSections"
        :total-questions-count="totalQuestionsCount"
        :difficulty-counts="difficultyCounts"
        :difficulty-percentages="difficultyPercentages"
        :covered-knowledge-points="coveredKnowledgePoints"
      />
    </div>

    <el-dialog v-model="exportDialogVisible" title="试卷数据导出" width="580px" destroy-on-close>
      <div v-loading="exportLoading" class="export-dialog-body">
        <p class="export-tip">
          您可以复制以下标准化试卷 JSON 结构用于系统间数据迁移或在线题库交换：
        </p>
        <el-input
          type="textarea"
          :rows="12"
          readonly
          :value="exportDataJson"
        />
      </div>
      <template #footer>
        <el-button @click="exportDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!exportDataJson" @click="handleCopyExportJson">
          复制 JSON 到剪贴板
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import ExamDetailHeroSection from '@/components/question/exam/ExamDetailHeroSection.vue';
import ExamDetailPaperDisplay from '@/components/question/exam/ExamDetailPaperDisplay.vue';
import ExamDetailStatsSidebar from '@/components/question/exam/ExamDetailStatsSidebar.vue';
import { useExamDetail } from '@/composables/question/useExam';

const {
  router,
  loading,
  examData,
  viewMode,
  exportDialogVisible,
  exportLoading,
  exportDataJson,
  groupedSections,
  totalQuestionsCount,
  difficultyCounts,
  difficultyPercentages,
  coveredKnowledgePoints,
  handlePublishAsAssignment,
  handleExportPaper,
  handleCopyExportJson,
  printPaper,
  getChineseNumber,
  getStatusLabel,
  getStatusTagType
} = useExamDetail();
</script>

<style scoped lang="scss">
.exam-detail-container {
  padding: 24px;
  background: #f8fafc;

  .main-content-layout {
    display: flex;
    gap: 24px;
    align-items: flex-start;
  }

  .export-dialog-body {
    .export-tip {
      font-size: 13px;
      color: #64748b;
      margin-bottom: 12px;
    }
  }
}
</style>

<style lang="scss">
@media print {
  body * {
    visibility: hidden;
  }
  #printable-exam-paper,
  #printable-exam-paper * {
    visibility: visible;
  }
  #printable-exam-paper {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    box-shadow: none !important;
    border: none !important;
  }
}
</style>
