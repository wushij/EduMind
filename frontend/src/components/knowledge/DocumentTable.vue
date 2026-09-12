<template>
  <div class="document-table-container">
    <el-table
      :data="documents"
      stripe
      style="width: 100%"
      class="custom-document-table"
      :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600' }"
    >
      <el-table-column prop="fileName" label="课件文件" min-width="260">
        <template #default="{ row }">
          <div class="file-name-cell">
            <div class="file-icon-badge" :class="getFileTypeClass(row.fileType || row.fileName)">
              {{ getFileExt(row.fileType || row.fileName) }}
            </div>
            <div class="file-info">
              <span class="file-title" :title="row.fileName">{{ row.fileName }}</span>
              <span class="file-meta">{{ formatFileSize(row.fileSize) }} · {{ row.createdAt || '近期上传' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="parseStatus" label="解析状态" width="130">
        <template #default="{ row }">
          <el-tag :type="getParseStatusType(row.parseStatus)" size="small" effect="light">
            {{ formatParseStatus(row.parseStatus) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="切片状态" width="160">
        <template #default="{ row }">
          <div class="chunk-status-cell">
            <span class="chunk-count-badge">{{ row.chunkCount ?? 0 }} Chunks</span>
            <el-tag
              :type="row.chunkStatus === 'INDEXED' ? 'success' : row.chunkStatus === 'CHUNKING' ? 'warning' : 'info'"
              size="small"
              class="mini-tag"
            >
              {{ row.chunkStatus === 'INDEXED' ? '已入库' : row.chunkStatus === 'CHUNKING' ? '切片中' : '已就绪' }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button link type="primary" size="small" @click="$emit('view-chunks', row.id)">
              查看切片
            </el-button>
            <el-button link type="primary" size="small" @click="$emit('rechunk', row.id)">
              重新切片
            </el-button>
            <el-button
              v-if="row.parseStatus !== 'PARSED'"
              link
              type="success"
              size="small"
              @click="$emit('parse', row.id)"
            >
              解析
            </el-button>
            <el-button link type="danger" size="small" @click="$emit('delete', row.id)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { KBDocument } from '@/types/knowledge/document';

defineProps<{ documents: KBDocument[] }>();
defineEmits<{
  (e: 'parse', id: number): void;
  (e: 'delete', id: number): void;
  (e: 'view-chunks', id: number): void;
  (e: 'rechunk', id: number): void;
}>();

const getFileExt = (fileName: string): string => {
  if (!fileName) return 'FILE';
  const ext = fileName.split('.').pop()?.toUpperCase();
  return ext || 'FILE';
};

const getFileTypeClass = (fileName: string): string => {
  const ext = getFileExt(fileName).toLowerCase();
  if (ext === 'pdf') return 'file-pdf';
  if (['doc', 'docx'].includes(ext)) return 'file-word';
  if (['ppt', 'pptx'].includes(ext)) return 'file-ppt';
  if (['md', 'markdown'].includes(ext)) return 'file-md';
  return 'file-default';
};

const formatFileSize = (sizeInBytes?: number): string => {
  if (!sizeInBytes) return '2.4 MB';
  if (sizeInBytes < 1024) return `${sizeInBytes} B`;
  if (sizeInBytes < 1024 * 1024) return `${(sizeInBytes / 1024).toFixed(1)} KB`;
  return `${(sizeInBytes / (1024 * 1024)).toFixed(1)} MB`;
};

const formatParseStatus = (status?: string): string => {
  switch (status) {
    case 'PARSED': return '已完成解析';
    case 'PARSING': return '正在解析';
    case 'FAILED': return '解析失败';
    default: return '待解析';
  }
};

const getParseStatusType = (status?: string): '' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (status) {
    case 'PARSED': return 'success';
    case 'PARSING': return 'warning';
    case 'FAILED': return 'danger';
    default: return 'info';
  }
};
</script>

<style scoped lang="scss">
.document-table-container {
  background: #FFFFFF;
  border-radius: 10px;
  overflow: hidden;

  .file-name-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .file-icon-badge {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 700;
      flex-shrink: 0;
      letter-spacing: 0.5px;

      &.file-pdf {
        background: #FEF2F2;
        color: #DC2626;
        border: 1px solid #FEE2E2;
      }
      &.file-word {
        background: #EFF6FF;
        color: #2563EB;
        border: 1px solid #DBEAFE;
      }
      &.file-ppt {
        background: #FFF7ED;
        color: #EA580C;
        border: 1px solid #FFEDD5;
      }
      &.file-md {
        background: #F8FAFC;
        color: #475569;
        border: 1px solid #E2E8F0;
      }
      &.file-default {
        background: #F1F5F9;
        color: #64748B;
      }
    }

    .file-info {
      display: flex;
      flex-direction: column;
      gap: 3px;
      min-width: 0;

      .file-title {
        font-size: 13.5px;
        font-weight: 600;
        color: #1E293B;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-meta {
        font-size: 11.5px;
        color: #94A3B8;
      }
    }
  }

  .chunk-status-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .chunk-count-badge {
      font-family: ui-monospace, monospace;
      font-size: 12px;
      color: #334155;
      background: #F1F5F9;
      padding: 2px 6px;
      border-radius: 4px;
    }

    .mini-tag {
      font-size: 11px;
    }
  }

  .action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
