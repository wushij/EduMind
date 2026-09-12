<template>
  <div v-if="citations && citations.length > 0" class="course-ai-citation-container">
    <div class="citation-header" @click="isExpanded = !isExpanded">
      <div class="header-left">
        <el-icon class="icon"><Document /></el-icon>
        <span class="label">参考课程资料来源 ({{ citations.length }} 处出处精准溯源)</span>
      </div>
      <div class="header-right">
        <span class="expand-text">{{ isExpanded ? '收起' : '展开查看' }}</span>
        <el-icon class="arrow" :class="{ 'rotate': isExpanded }"><ArrowDown /></el-icon>
      </div>
    </div>

    <!-- 展开后的引用卡片列表 -->
    <div v-show="isExpanded" class="citations-body">
      <div
        v-for="(item, idx) in citations"
        :key="item.id || idx"
        class="citation-chip-card"
        @click="openSnippetDialog(item)"
      >
        <div class="chip-top">
          <span class="cite-num">[{{ idx + 1 }}]</span>
          <span class="doc-name" :title="item.docTitle || item.documentName">
            {{ item.docTitle || item.documentName || '课程核心教材' }}
          </span>
          <span v-if="item.page || item.pageNo" class="page-tag">
            P.{{ item.page || item.pageNo }}
          </span>
          <span v-if="item.score" class="score-tag">
            {{ (item.score * 100).toFixed(0) }}% 匹配
          </span>
        </div>
        <p v-if="item.snippet || item.excerpt" class="snippet-preview">
          {{ item.snippet || item.excerpt }}
        </p>
      </div>
    </div>

    <!-- 引用切片弹窗预览 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`教材出处引用 · ${activeCitation?.docTitle || activeCitation?.documentName || '课程资料'}`"
      width="540px"
      append-to-body
      class="citation-modal"
    >
      <div v-if="activeCitation" class="modal-inner">
        <div class="meta-row">
          <span class="tag page">页码：P.{{ activeCitation.page || activeCitation.pageNo || 1 }}</span>
          <span class="tag score">知识库置信度：{{ activeCitation.score ? (activeCitation.score * 100).toFixed(1) + '%' : '高' }}</span>
        </div>
        <div class="modal-quote-box">
          {{ activeCitation.snippet || activeCitation.excerpt || activeCitation.content }}
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Document, ArrowDown } from '@element-plus/icons-vue';

export interface CitationItem {
  id?: number | string;
  docTitle?: string;
  documentName?: string;
  page?: number;
  pageNo?: number;
  snippet?: string;
  excerpt?: string;
  content?: string;
  score?: number;
}

defineProps<{
  citations: CitationItem[];
}>();

const isExpanded = ref(false);
const dialogVisible = ref(false);
const activeCitation = ref<CitationItem | null>(null);

const openSnippetDialog = (item: CitationItem) => {
  activeCitation.value = item;
  dialogVisible.value = true;
};
</script>

<style scoped lang="scss">
.course-ai-citation-container {
  margin-top: 12px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s ease;

  .citation-header {
    padding: 8px 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    user-select: none;
    background: #F1F5F9;

    &:hover {
      background: #E2E8F0;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: #334155;

      .icon {
        color: #2563EB;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #64748B;

      .arrow {
        transition: transform 0.2s;
        &.rotate {
          transform: rotate(180deg);
        }
      }
    }
  }

  .citations-body {
    padding: 10px 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .citation-chip-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 6px;
      padding: 8px 10px;
      cursor: pointer;
      transition: all 0.15s;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 2px 6px rgba(37, 99, 235, 0.06);
      }

      .chip-top {
        display: flex;
        align-items: center;
        gap: 6px;

        .cite-num {
          font-weight: 700;
          color: #2563EB;
          font-size: 11px;
        }

        .doc-name {
          font-size: 12px;
          font-weight: 600;
          color: #1E293B;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 220px;
        }

        .page-tag {
          font-family: ui-monospace, monospace;
          font-size: 10px;
          background: #FEF3C7;
          color: #92400E;
          padding: 1px 4px;
          border-radius: 3px;
        }

        .score-tag {
          margin-left: auto;
          font-family: ui-monospace, monospace;
          font-size: 10.5px;
          color: #059669;
          font-weight: 600;
        }
      }

      .snippet-preview {
        margin: 4px 0 0 0;
        font-size: 11.5px;
        color: #64748B;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .modal-inner {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .meta-row {
      display: flex;
      gap: 10px;

      .tag {
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 4px;

        &.page {
          background: #EFF6FF;
          color: #2563EB;
          font-weight: 600;
        }

        &.score {
          background: #ECFDF5;
          color: #059669;
        }
      }
    }

    .modal-quote-box {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 16px;
      font-size: 13.5px;
      line-height: 1.7;
      color: #1E293B;
      white-space: pre-wrap;
      max-height: 360px;
      overflow-y: auto;
    }
  }
}
</style>
