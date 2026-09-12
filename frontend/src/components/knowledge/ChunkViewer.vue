<template>
  <el-drawer
    :model-value="visible"
    :title="`切片详情 · #${chunk?.chunkIndex ?? 0}`"
    size="520px"
    destroy-on-close
    class="chunk-viewer-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="chunk" class="chunk-viewer-body">
      <!-- 头部元数据看板 -->
      <div class="meta-badge-group">
        <div class="meta-item">
          <span class="label">文档来源</span>
          <span class="value doc-name" :title="chunk.documentName">{{ chunk.documentName || `文档 #${chunk.documentId}` }}</span>
        </div>
        <div class="meta-item">
          <span class="label">页码位置</span>
          <span class="value badge-tag page-tag">P.{{ chunk.pageNo || 1 }}</span>
        </div>
        <div class="meta-item">
          <span class="label">Token 估算</span>
          <span class="value badge-tag token-tag">{{ chunk.tokenCount }} Tokens</span>
        </div>
        <div class="meta-item">
          <span class="label">向量状态</span>
          <el-tag
            :type="chunk.status === 'INDEXED' ? 'success' : chunk.status === 'PENDING' ? 'warning' : 'danger'"
            effect="light"
            size="small"
            class="status-tag"
          >
            {{ chunk.status === 'INDEXED' ? '已向量化' : chunk.status === 'PENDING' ? '排队索引中' : '索引失败' }}
          </el-tag>
        </div>
      </div>

      <!-- 章节标题 -->
      <div v-if="chunk.heading" class="section-card">
        <div class="section-title">
          <el-icon class="title-icon"><CollectionTag /></el-icon>
          <span>大纲定位</span>
        </div>
        <div class="heading-content">{{ chunk.heading }}</div>
      </div>

      <!-- 正文内容展示区 -->
      <div class="content-section">
        <div class="section-header">
          <div class="title">
            <el-icon class="title-icon"><Document /></el-icon>
            <span>切片分块正文</span>
          </div>
          <el-button
            size="small"
            type="primary"
            link
            :icon="CopyDocument"
            @click="copyContent"
          >
            复制正文
          </el-button>
        </div>
        <div class="chunk-content-box">
          {{ chunk.content }}
        </div>
      </div>

      <!-- 底层切片工程参数 -->
      <div class="tech-spec-section">
        <div class="spec-title">底层工程参数</div>
        <div class="spec-grid">
          <div class="spec-row">
            <span class="k">分块索引 ID</span>
            <span class="v mono">{{ chunk.id }}</span>
          </div>
          <div class="spec-row">
            <span class="k">字符总长度</span>
            <span class="v">{{ chunk.charCount || chunk.content.length }} 字符</span>
          </div>
          <div class="spec-row">
            <span class="k">更新同步时间</span>
            <span class="v">{{ chunk.updatedAt || chunk.createdAt || '2026-09-11 10:00:00' }}</span>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { DocumentChunk } from '@/types/knowledge/chunk';
import { ElMessage } from 'element-plus';
import { CollectionTag, Document, CopyDocument } from '@element-plus/icons-vue';

const props = defineProps<{
  visible: boolean;
  chunk: DocumentChunk | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
}>();

const copyContent = async () => {
  if (!props.chunk?.content) return;
  try {
    await navigator.clipboard.writeText(props.chunk.content);
    ElMessage.success('切片正文已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败，请手动选取');
  }
};
</script>

<style scoped lang="scss">
.chunk-viewer-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #1E293B;

  .meta-badge-group {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 14px 16px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px 16px;

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .label {
        font-size: 11px;
        color: #94A3B8;
        font-weight: 500;
      }

      .value {
        font-size: 13px;
        font-weight: 600;
        color: #334155;

        &.doc-name {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        &.badge-tag {
          display: inline-block;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          padding: 2px 8px;
          border-radius: 6px;
          width: fit-content;
        }

        &.page-tag {
          background: #EFF6FF;
          color: #2563EB;
        }

        &.token-tag {
          background: #F0FDF4;
          color: #16A34A;
        }
      }
    }
  }

  .section-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 12px 16px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: #64748B;
      margin-bottom: 6px;

      .title-icon {
        color: #3B82F6;
      }
    }

    .heading-content {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
    }
  }

  .content-section {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
        color: #475569;

        .title-icon {
          color: #2563EB;
        }
      }
    }

    .chunk-content-box {
      background: #FAFBFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 16px;
      font-size: 13.5px;
      line-height: 1.7;
      color: #1E293B;
      white-space: pre-wrap;
      word-break: break-word;
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
    }
  }

  .tech-spec-section {
    border-top: 1px dashed #E2E8F0;
    padding-top: 14px;

    .spec-title {
      font-size: 11px;
      color: #94A3B8;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 10px;
    }

    .spec-grid {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .spec-row {
        display: flex;
        justify-content: space-between;
        font-size: 12px;

        .k {
          color: #64748B;
        }

        .v {
          color: #334155;
          font-weight: 500;

          &.mono {
            font-family: ui-monospace, monospace;
          }
        }
      }
    }
  }
}
</style>
