<template>
  <div class="export-center-page" v-loading="loading">
    <PageHeroBanner
      title="试卷制卷与考务排版工坊 · 高保真排版与打印"
      subtitle="支持 A4单栏 / B4双栏中缝规范、密封装订线、考生条形码、大题赋分表、防伪动态水印与机读答题卡一键生成"
      background-variant="question"
    >
      <template #extra>
        <ExportHeroStats />
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <ExportPresetBar
        :preset-templates="presetTemplates"
        :current-preset-id="currentPresetId"
        :apply-preset="applyPreset"
      />

      <div class="export-split-workspace">
        <ExportControlPanel
          :config-form="configForm"
          v-model:active-tab="activeTab"
          v-model:exam-time-score="examTimeScore"
          :exporting="exporting"
          :handle-print-direct="handlePrintDirect"
          :handle-create-export-task="handleCreateExportTask"
          :handle-export-word="handleExportWord"
        />

        <ExportPreviewCanvas
          :config-form="configForm"
          v-model:preview-page-mode="previewPageMode"
          v-model:zoom-scale="zoomScale"
          v-model:print-scope="printScope"
          :handle-print-direct="handlePrintDirect"
        />
      </div>

      <ExportJobTable
        :export-history="exportHistory"
        :refresh-history="refreshHistory"
        :download-file="downloadFile"
        :handle-print-direct="handlePrintDirect"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import ExportHeroStats from '@/components/question/export/ExportHeroStats.vue';
import ExportPresetBar from '@/components/question/export/ExportPresetBar.vue';
import ExportControlPanel from '@/components/question/export/ExportControlPanel.vue';
import ExportPreviewCanvas from '@/components/question/export/ExportPreviewCanvas.vue';
import ExportJobTable from '@/components/question/export/ExportJobTable.vue';
import { useExport } from '@/composables/question/useExport';

const {
  loading,
  exporting,
  zoomScale,
  examTimeScore,
  activeTab,
  previewPageMode,
  printScope,
  currentPresetId,
  presetTemplates,
  configForm,
  exportHistory,
  applyPreset,
  handleCreateExportTask,
  handleExportWord,
  handlePrintDirect,
  refreshHistory,
  downloadFile
} = useExport();
</script>

<style scoped lang="scss">
.export-center-page {
  padding-bottom: 40px;
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
