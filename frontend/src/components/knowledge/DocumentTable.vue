<template>
  <div class="document-table-container">
    <el-table
      :data="documents"
      stripe
      style="width: 100%"
      class="custom-document-table"
      :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600' }"
      empty-text="暂无文档，请先上传课件"
    >
      <el-table-column prop="fileName" label="课件文件" min-width="300">
        <template #default="{ row }">
          <div class="file-name-cell">
            <DocumentFileIcon :kind="resolveDocumentFileKind(row.fileName, row.fileType)" />
            <div class="file-info">
              <span class="file-title" :title="row.fileName">{{ row.fileName }}</span>
              <span class="file-meta">{{ formatSize(row.fileSize) }} · {{ formatUploadTime(row.createTime || row.createdAt) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="parseStatus" label="解析状态" width="140" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="`status-pill--${getParseStatusTagType(row.parseStatus) || 'info'}`">
            {{ formatParseStatusLabel(row.parseStatus) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="切片状态" width="190" align="center">
        <template #default="{ row }">
          <div class="chunk-status-cell">
            <span class="chunk-count-badge">{{ row.chunkCount ?? 0 }} Chunks</span>
            <span class="status-pill status-pill--mini" :class="`status-pill--${getChunkStatusTagType(row.chunkStatus) || 'info'}`">
              {{ formatChunkStatusLabel(row.chunkStatus, row.chunkCount ?? 0) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="300" width="300" fixed="right" align="center" class-name="action-column">
        <template #default="{ row }">
          <div class="action-pill-group">
            <button
              type="button"
              class="table-action-pill table-action-pill--primary"
              @click="$emit('view-chunks', row.id)"
            >
              查看切片
            </button>
            <button
              type="button"
              class="table-action-pill table-action-pill--primary"
              @click="$emit('rechunk', row.id)"
            >
              重新切片
            </button>
            <button
              v-if="canTriggerParse(row.parseStatus)"
              type="button"
              class="table-action-pill table-action-pill--success"
              @click="$emit('parse', row.id)"
            >
              解析
            </button>
            <button
              type="button"
              class="table-action-pill table-action-pill--danger"
              @click="$emit('delete', row.id)"
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
import DocumentFileIcon from '@/components/knowledge/DocumentFileIcon.vue';
import { KBDocument } from '@/types/knowledge/document';
import { formatFileSize } from '@/utils/format/file';
import { formatRelativeTime } from '@/utils/format/date';
import {
  canTriggerParse,
  formatChunkStatusLabel,
  formatParseStatusLabel,
  getChunkStatusTagType,
  getParseStatusTagType,
  resolveDocumentFileKind
} from '@/utils/knowledge/document';

defineProps<{ documents: KBDocument[] }>();
defineEmits<{
  (e: 'parse', id: number): void;
  (e: 'delete', id: number): void;
  (e: 'view-chunks', id: number): void;
  (e: 'rechunk', id: number): void;
}>();

const formatSize = (sizeInBytes?: number): string => {
  if (sizeInBytes == null || Number.isNaN(sizeInBytes)) return '大小未知';
  if (sizeInBytes <= 0) return '0 B';
  return formatFileSize(sizeInBytes);
};

const formatUploadTime = (time?: string): string => {
  if (!time) return '上传时间未知';
  const relative = formatRelativeTime(time);
  return relative || time;
};
</script>

<style scoped lang="scss">
.document-table-container {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #edf2f7;

  :deep(.custom-document-table) {
    --el-table-border-color: #edf2f7;
    --el-table-row-hover-bg-color: #f8fbff;

    .el-table__header-wrapper th.el-table__cell {
      height: 46px;
      font-size: 13px;
    }

    .el-table__body-wrapper .el-table__cell {
      padding-top: 14px;
      padding-bottom: 14px;
      vertical-align: middle;
    }

    .action-column .cell {
      display: flex;
      align-items: center;
      justify-content: center;
      padding-top: 10px;
      padding-bottom: 10px;
    }
  }

  .file-name-cell {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;

    .file-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;

      .file-title {
        font-size: 14px;
        font-weight: 600;
        color: #0f172a;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-meta {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }

  .chunk-status-cell {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    flex-wrap: nowrap;

    .chunk-count-badge {
      font-family: ui-monospace, monospace;
      font-size: 12px;
      color: #334155;
      background: #f1f5f9;
      padding: 3px 8px;
      border-radius: 9999px;
      border: 1px solid #e2e8f0;
      white-space: nowrap;
    }
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 26px;
    padding: 0 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    border: 1px solid transparent;
    white-space: nowrap;

    &--mini {
      height: 24px;
      padding: 0 10px;
      font-size: 11px;
    }

    &--success {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #059669;
    }

    &--warning {
      background: #fffbeb;
      border-color: #fde68a;
      color: #d97706;
    }

    &--danger {
      background: #fef2f2;
      border-color: #fecaca;
      color: #dc2626;
    }

    &--info {
      background: #f8fafc;
      border-color: #e2e8f0;
      color: #64748b;
    }
  }

  .action-pill-group {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-wrap: nowrap;
    gap: 8px;
    width: 100%;
  }
}

.table-action-pill {
  height: 30px;
  padding: 0 14px;
  border-radius: 9999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;

  &--primary {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #2563eb;

    &:hover {
      background: #2563eb;
      border-color: #2563eb;
      color: #ffffff;
    }
  }

  &--success {
    background: #ecfdf5;
    border-color: #a7f3d0;
    color: #059669;

    &:hover {
      background: #059669;
      border-color: #059669;
      color: #ffffff;
    }
  }

  &--danger {
    background: #fef2f2;
    border-color: #fecaca;
    color: #dc2626;

    &:hover {
      background: #dc2626;
      border-color: #dc2626;
      color: #ffffff;
    }
  }
}
</style>
