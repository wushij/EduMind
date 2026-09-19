<template>
  <div class="export-center-page question-module-page" v-loading="loading">
    <ModulePageHeroHeader
      :icon="DocumentCopy"
      title="试卷制卷与考务排版工坊"
      badge="高保真排版与打印"
      description="左侧可实时预览卷面。「生成 PDF」会在服务端异步排版（非浏览器直接导出），完成后自动下载；进度见下方蓝色提示条与任务列表。"
    >
      <template #stats>
        <ExportHeroStats
          :exam-paper="examPaper"
          :total-questions-count="totalQuestionsCount"
          :last-export-task="lastExportTask"
        />
      </template>
      <template #actions>
        <button type="button" class="module-capsule-btn module-capsule-btn--secondary" @click="refreshHistory">
          <el-icon><Refresh /></el-icon>
          <span>刷新任务</span>
        </button>
        <button
          type="button"
          class="module-capsule-btn module-capsule-btn--primary"
          :disabled="exporting"
          @click="handleCreateExportTask"
        >
          <el-icon v-if="exporting" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Download /></el-icon>
          <span>{{ exporting ? '排版生成中…' : '生成 PDF' }}</span>
        </button>
      </template>
    </ModulePageHeroHeader>

    <div class="main-content-layout">
      <ExportActiveTaskBanner
        :task="activeExportTask"
        @download="downloadActiveExportTask"
        @view-jobs="scrollToExportJobs"
        @dismiss="dismissActiveExportTask"
      />

      <ExportPresetBar
        :preset-templates="presetTemplates"
        :current-preset-id="currentPresetId"
        :apply-preset="applyPreset"
      />

      <div class="export-split-workspace">
        <ExportControlPanel
          :config-form="configForm"
          :exam-options="examOptions"
          :exams-loading="examsLoading"
          v-model:active-tab="activeTab"
          v-model:exam-time-score="examTimeScore"
          :exporting="exporting"
          :handle-print-direct="handlePrintDirect"
          :handle-create-export-task="handleCreateExportTask"
          :handle-export-word="handleExportWord"
        />

        <ExportPreviewCanvas
          :config-form="configForm"
          :exam-paper="examPaper"
          :grouped-sections="groupedSections"
          :total-questions-count="totalQuestionsCount"
          :objective-question-count="objectiveQuestionCount"
          :preview-loading="previewLoading"
          :preview-error="previewError"
          v-model:preview-page-mode="previewPageMode"
          v-model:zoom-scale="zoomScale"
          v-model:print-scope="printScope"
          :handle-print-direct="handlePrintDirect"
        />
      </div>

      <ExportJobTable
        :export-history="exportHistory"
        v-model:page-num="exportHistoryPageNum"
        v-model:page-size="exportHistoryPageSize"
        :total="exportHistoryTotal"
        :refresh-history="refreshHistory"
        :download-file="downloadFile"
        :handle-print-direct="handlePrintDirect"
        :handle-delete-export-task="handleDeleteExportTask"
        @page-change="onExportHistoryPageChange"
      />
    </div>

    <ExportPrintDialog
      v-model="printDialogVisible"
      :current-view-only="printScope === 'current'"
      @confirm="confirmPrintFromDialog"
    />
  </div>
</template>

<script setup lang="ts">
import { DocumentCopy, Download, Refresh, Loading } from '@element-plus/icons-vue';
import ExportActiveTaskBanner from '@/components/question/export/ExportActiveTaskBanner.vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import ExportHeroStats from '@/components/question/export/ExportHeroStats.vue';
import ExportPresetBar from '@/components/question/export/ExportPresetBar.vue';
import ExportControlPanel from '@/components/question/export/ExportControlPanel.vue';
import ExportPreviewCanvas from '@/components/question/export/ExportPreviewCanvas.vue';
import ExportJobTable from '@/components/question/export/ExportJobTable.vue';
import ExportPrintDialog from '@/components/question/export/ExportPrintDialog.vue';
import { useExport } from '@/composables/question/useExport';

const {
  loading,
  previewLoading,
  previewError,
  exporting,
  activeExportTask,
  zoomScale,
  examTimeScore,
  activeTab,
  previewPageMode,
  printScope,
  currentPresetId,
  presetTemplates,
  configForm,
  exportHistory,
  exportHistoryPageNum,
  exportHistoryPageSize,
  exportHistoryTotal,
  onExportHistoryPageChange,
  examPaper,
  examOptions,
  examsLoading,
  groupedSections,
  totalQuestionsCount,
  objectiveQuestionCount,
  lastExportTask,
  applyPreset,
  handleCreateExportTask,
  handleExportWord,
  handlePrintDirect,
  printDialogVisible,
  confirmPrintFromDialog,
  refreshHistory,
  downloadFile,
  handleDeleteExportTask,
  scrollToExportJobs,
  dismissActiveExportTask,
  downloadActiveExportTask
} = useExport();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.export-center-page {
  padding-bottom: 8px;

  > * {
    flex: 0 0 auto;
  }
}

.main-content-layout {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.export-split-workspace {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  width: 100%;

  @media (max-width: 1180px) {
    grid-template-columns: 1fr;
  }
}
</style>
