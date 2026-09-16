<template>
  <div class="export-history-card no-print">
    <div class="history-header">
      <div class="history-title-group">
        <el-icon class="history-icon"><Clock /></el-icon>
        <h3>近期试卷导出任务与云端归档</h3>
      </div>
      <div class="history-actions">
        <el-button round size="small" class="btn-refresh" :icon="Refresh" @click="refreshHistory">
          刷新任务状态
        </el-button>
      </div>
    </div>

    <el-table :data="exportHistory" stripe style="width: 100%;">
      <el-table-column prop="taskId" label="任务编号" width="170" />
      <el-table-column prop="title" label="试卷名称" min-width="260" />
      <el-table-column prop="size" label="纸张幅面" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.size === 'B4' ? 'warning' : 'info'">{{ row.size }} 考卷</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="导出格式" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="primary" effect="plain">{{ row.type || 'PDF' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'SUCCESS'" type="success" size="small">导出成功</el-tag>
          <el-tag v-else-if="row.status === 'PROCESSING'" type="warning" size="small">生成中 ({{ row.progress || 50 }}%)</el-tag>
          <el-tag v-else-if="row.status === 'PENDING'" type="info" size="small">排队中</el-tag>
          <el-tooltip v-else-if="row.status === 'FAILED'" :content="row.errorMsg || '生成失败'" placement="top">
            <el-tag type="danger" size="small">生成失败</el-tag>
          </el-tooltip>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="生成时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="action-pill-group">
            <button
              type="button"
              class="table-action-pill table-action-pill--primary"
              :disabled="row.status !== 'SUCCESS' || !row.downloadUrl"
              @click="downloadFile(row)"
            >
              下载
            </button>
            <button type="button" class="table-action-pill table-action-pill--success" @click="handlePrintDirect">
              打印
            </button>
            <button
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click="handleDeleteExportTask(row)"
            >
              删除
            </button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { Download, Printer, Clock, Refresh } from '@element-plus/icons-vue';
import type { ExportHistoryItem } from '@/composables/question/useExport';

defineProps<{
  exportHistory: ExportHistoryItem[];
  refreshHistory: () => void;
  downloadFile: (row: ExportHistoryItem) => void;
  handlePrintDirect: () => void;
  handleDeleteExportTask: (row: ExportHistoryItem) => void;
}>();
</script>

<style scoped lang="scss">
.export-history-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 22px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

  .history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .history-title-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .history-icon {
        font-size: 18px;
        color: #2563EB;
      }

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #0F172A;
        margin: 0;
      }
    }
  }
}
</style>
